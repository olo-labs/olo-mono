package org.olo.kernel.dynamicgraph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates and merges model-produced subgraph JSON into the active workflow graph.
 */
public final class DynamicSubgraphMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DynamicSubgraphMerger() {
    }

    public static ValidationResult validate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ValidationResult.invalid("generated graph JSON is blank");
        }
        try {
            String normalized = stripMarkdownFences(rawJson.trim());
            JsonNode root = MAPPER.readTree(normalized);
            if (!root.isObject()) {
                return ValidationResult.invalid("generated graph JSON must be a single object");
            }
            if (!root.hasNonNull("id") || root.get("id").asText().isBlank()) {
                return ValidationResult.invalid("generated graph JSON requires non-blank id");
            }
            JsonNode nodes = root.get("nodes");
            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                return ValidationResult.invalid("generated graph JSON requires a non-empty nodes array");
            }
            JsonNode edges = root.get("edges");
            if (edges == null || !edges.isArray()) {
                return ValidationResult.invalid("generated graph JSON requires an edges array");
            }
            Set<String> nodeIds = new LinkedHashSet<>();
            boolean hasStart = false;
            boolean hasEnd = false;
            for (JsonNode node : nodes) {
                if (!node.isObject()) {
                    return ValidationResult.invalid("each node entry must be an object");
                }
                String id = text(node, "id");
                String type = text(node, "type");
                if (id == null || id.isBlank() || type == null || type.isBlank()) {
                    return ValidationResult.invalid("each node requires id and type");
                }
                if (!nodeIds.add(id)) {
                    return ValidationResult.invalid("duplicate node id in generated graph JSON: " + id);
                }
                if (NodeType.START.name().equals(type)) {
                    hasStart = true;
                }
                if (NodeType.END.name().equals(type)) {
                    hasEnd = true;
                }
                if (NodeType.TOOL.name().equals(type)) {
                    JsonNode configuration = node.get("configuration");
                    String toolId = configuration == null ? null : text(configuration, "toolId");
                    if (toolId == null || toolId.isBlank()) {
                        return ValidationResult.invalid("TOOL node '" + id + "' requires configuration.toolId");
                    }
                }
                if (!isAllowedNodeType(type)) {
                    return ValidationResult.invalid(
                            "invalid node type '"
                                    + type
                                    + "' for node '"
                                    + id
                                    + "'. Use a single NodeType value such as START, END, AGENT, or TOOL");
                }
            }
            if (!hasStart || !hasEnd) {
                return ValidationResult.invalid("generated graph JSON must include START and END nodes");
            }
            for (JsonNode edge : edges) {
                if (!edge.isObject()) {
                    return ValidationResult.invalid("each edge entry must be an object");
                }
                String sourceNodeId = text(edge, "sourceNodeId");
                String targetNodeId = text(edge, "targetNodeId");
                if (sourceNodeId == null
                        || sourceNodeId.isBlank()
                        || targetNodeId == null
                        || targetNodeId.isBlank()) {
                    return ValidationResult.invalid("each edge requires sourceNodeId and targetNodeId");
                }
                if (!nodeIds.contains(sourceNodeId) || !nodeIds.contains(targetNodeId)) {
                    return ValidationResult.invalid("edge references unknown node id");
                }
            }
            return ValidationResult.valid(normalized);
        } catch (Exception e) {
            return ValidationResult.invalid("generated graph JSON is not valid JSON: " + e.getMessage());
        }
    }

    public static MergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            String validatedJson) {
        try {
            JsonNode root = MAPPER.readTree(validatedJson);
            List<NodeDefinition> subgraphNodes = parseNodes(root.get("nodes"));
            List<EdgeDefinition> subgraphEdges = parseEdges(root.get("edges"));

            String startId = subgraphNodes.stream()
                    .filter(node -> NodeType.START.name().equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElseThrow();
            String endId = subgraphNodes.stream()
                    .filter(node -> NodeType.END.name().equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElseThrow();

            String entryNodeId = subgraphEdges.stream()
                    .filter(edge -> startId.equals(edge.getSourceNodeId()))
                    .map(EdgeDefinition::getTargetNodeId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("subgraph START has no outgoing edge"));

            List<String> exitNodeIds = subgraphEdges.stream()
                    .filter(edge -> endId.equals(edge.getTargetNodeId()))
                    .map(EdgeDefinition::getSourceNodeId)
                    .distinct()
                    .toList();
            if (exitNodeIds.isEmpty()) {
                throw new IllegalStateException("subgraph END has no incoming edge");
            }

            String prefix = "dyn-" + System.nanoTime() + "-";
            Map<String, String> idMap = new LinkedHashMap<>();
            for (NodeDefinition node : subgraphNodes) {
                if (NodeType.START.name().equals(node.getType()) || NodeType.END.name().equals(node.getType())) {
                    continue;
                }
                idMap.put(node.getId(), prefix + node.getId());
            }

            WorkflowBuilder builder = WorkflowBuilder.from(graph);
            List<EdgeDefinition> mergedEdges = new ArrayList<>();
            for (EdgeDefinition edge : graph.getEdges()) {
                if (plannerNodeId.equals(edge.getSourceNodeId()) && continueNodeId.equals(edge.getTargetNodeId())) {
                    continue;
                }
                mergedEdges.add(edge);
            }

            for (NodeDefinition node : subgraphNodes) {
                if (NodeType.START.name().equals(node.getType()) || NodeType.END.name().equals(node.getType())) {
                    continue;
                }
                NodeDefinition remapped = withDefaultPorts(
                        DynamicNodeLabels.withDynamicLabel(remapNode(node, idMap.get(node.getId())), graph));
                builder.addNode(remapped);
            }

            for (EdgeDefinition edge : subgraphEdges) {
                if (startId.equals(edge.getSourceNodeId()) || endId.equals(edge.getTargetNodeId())) {
                    continue;
                }
                mergedEdges.add(remapEdge(edge, idMap));
            }

            String prefixedEntry = idMap.get(entryNodeId);
            mergedEdges.add(EdgeDefinition.builder()
                    .sourceNodeId(plannerNodeId)
                    .sourcePortId("out")
                    .targetNodeId(prefixedEntry)
                    .targetPortId("in")
                    .build());
            for (String exitNodeId : exitNodeIds) {
                mergedEdges.add(EdgeDefinition.builder()
                        .sourceNodeId(idMap.get(exitNodeId))
                        .sourcePortId("out")
                        .targetNodeId(continueNodeId)
                        .targetPortId("in")
                        .build());
            }

            builder.replaceEdges(mergedEdges);
            return new MergeResult(builder.build(), prefixedEntry);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("failed to merge generated subgraph: " + e.getMessage(), e);
        }
    }

    public static int readRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        Object raw = variables.get(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    public static void incrementRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        variables.set(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE, readRetryCount(variables) + 1);
    }

    public static void resetRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        variables.set(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE, 0);
    }

    private static List<NodeDefinition> parseNodes(JsonNode nodes) {
        List<NodeDefinition> parsed = new ArrayList<>();
        for (JsonNode node : nodes) {
            NodeDefinition.Builder builder = NodeDefinition.builder()
                    .id(text(node, "id"))
                    .type(text(node, "type"));
            String label = text(node, "label");
            if (label != null && !label.isBlank()) {
                builder.label(label);
            }
            JsonNode configuration = node.get("configuration");
            if (configuration != null && configuration.isObject()) {
                builder.configuration(MAPPER.convertValue(configuration, Map.class));
            }
            parsed.add(builder.build());
        }
        return parsed;
    }

    private static List<EdgeDefinition> parseEdges(JsonNode edges) {
        List<EdgeDefinition> parsed = new ArrayList<>();
        for (JsonNode edge : edges) {
            parsed.add(EdgeDefinition.builder()
                    .sourceNodeId(text(edge, "sourceNodeId"))
                    .sourcePortId(Optional.ofNullable(text(edge, "sourcePortId")).orElse("out"))
                    .targetNodeId(text(edge, "targetNodeId"))
                    .targetPortId(Optional.ofNullable(text(edge, "targetPortId")).orElse("in"))
                    .build());
        }
        return parsed;
    }

    private static NodeDefinition remapNode(NodeDefinition node, String newId) {
        NodeDefinition.Builder builder = NodeDefinition.builder()
                .id(newId)
                .type(node.getType())
                .configuration(node.getConfiguration());
        if (node.getLabel() != null && !node.getLabel().isBlank()) {
            builder.label(node.getLabel());
        }
        return builder.build();
    }

    private static EdgeDefinition remapEdge(EdgeDefinition edge, Map<String, String> idMap) {
        return EdgeDefinition.builder()
                .sourceNodeId(idMap.get(edge.getSourceNodeId()))
                .sourcePortId(edge.getSourcePortId())
                .targetNodeId(idMap.get(edge.getTargetNodeId()))
                .targetPortId(edge.getTargetPortId())
                .build();
    }

    private static NodeDefinition withDefaultPorts(NodeDefinition node) {
        if (node.getPorts() != null && !node.getPorts().isEmpty()) {
            return node;
        }
        NodeDefinition.Builder builder = NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(node.getLabel())
                .configuration(node.getConfiguration());
        if (!NodeType.END.name().equals(node.getType())) {
            builder.addPort(PortDefinition.builder()
                    .id("out")
                    .name("out")
                    .schema("any")
                    .direction(PortDirection.OUTPUT)
                    .build());
        }
        if (!NodeType.START.name().equals(node.getType())) {
            builder.addPort(PortDefinition.builder()
                    .id("in")
                    .name("in")
                    .schema("any")
                    .direction(PortDirection.INPUT)
                    .build());
        }
        return builder.build();
    }

    private static String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            int closingFence = text.lastIndexOf("```");
            if (firstLineBreak >= 0 && closingFence > firstLineBreak) {
                return text.substring(firstLineBreak + 1, closingFence).trim();
            }
        }
        return text;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static boolean isAllowedNodeType(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        try {
            NodeType.valueOf(type.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public record ValidationResult(boolean valid, String normalizedJson, String message) {
        public static ValidationResult valid(String normalizedJson) {
            return new ValidationResult(true, normalizedJson, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, null, message);
        }
    }

    public record MergeResult(WorkflowDefinition graph, String entryNodeId) {
    }
}
