package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.dynamicgraph.ToolSynthesisSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.dynamicgraph.DynamicNodeLabels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates model-produced tool-call JSON and merges an inline tool execution subgraph after the planner node.
 */
public final class ToolCallSubgraphMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolCallSubgraphMerger() {
    }

    public static ValidationResult validate(String rawJson, String allowedToolsJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ValidationResult.invalid("tool call sequence JSON is blank");
        }
        try {
            String normalized = stripMarkdownFences(rawJson.trim());
            JsonNode root = MAPPER.readTree(normalized);
            if (!root.isObject()) {
                return ValidationResult.invalid("tool call sequence JSON must be a single object");
            }
            JsonNode toolCalls = root.get("toolCalls");
            if (toolCalls == null || !toolCalls.isArray()) {
                return ValidationResult.invalid("tool call sequence JSON requires a toolCalls array");
            }
            if (!root.has("directResponse")) {
                return ValidationResult.invalid("tool call sequence JSON requires directResponse (string or null)");
            }
            JsonNode directResponseNode = root.get("directResponse");
            String directResponse = directResponseNode == null || directResponseNode.isNull()
                    ? null
                    : directResponseNode.asText();
            Set<String> allowedToolIds = parseAllowedToolIds(allowedToolsJson);
            List<ParsedToolCall> parsedCalls = new ArrayList<>();
            for (JsonNode call : toolCalls) {
                if (!call.isObject()) {
                    return ValidationResult.invalid("each toolCalls entry must be an object");
                }
                String toolId = text(call, "toolId");
                if (toolId == null || toolId.isBlank()) {
                    return ValidationResult.invalid("each toolCalls entry requires toolId");
                }
                if (!allowedToolIds.isEmpty() && !allowedToolIds.contains(toolId)) {
                    return ValidationResult.invalid("toolId is not in availableToolsJson allow-list: " + toolId);
                }
                Map<String, Object> arguments = Map.of();
                JsonNode argumentsNode = call.get("arguments");
                if (argumentsNode != null && !argumentsNode.isNull()) {
                    if (!argumentsNode.isObject()) {
                        return ValidationResult.invalid("toolCalls.arguments must be an object when present");
                    }
                    arguments = MAPPER.convertValue(argumentsNode, Map.class);
                }
                parsedCalls.add(new ParsedToolCall(toolId, arguments));
            }
            if (!parsedCalls.isEmpty()) {
                if (directResponse != null && !directResponse.isBlank()) {
                    return ValidationResult.invalid(
                            "directResponse must be null when toolCalls is non-empty");
                }
                return ValidationResult.toolCalls(normalized, parsedCalls);
            }
            if (directResponse == null || directResponse.isBlank()) {
                return ValidationResult.invalid(
                        "directResponse is required when toolCalls is empty");
            }
            return ValidationResult.directResponse(normalized, directResponse);
        } catch (Exception e) {
            return ValidationResult.invalid("tool call sequence JSON is not valid JSON: " + e.getMessage());
        }
    }

    public static MergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedToolCall> toolCalls) {
        String prefix = "tool-dyn-" + System.nanoTime() + "-";
        List<String> dynamicNodeIds = new ArrayList<>();
        WorkflowBuilder builder = WorkflowBuilder.from(graph);
        List<EdgeDefinition> mergedEdges = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (plannerNodeId.equals(edge.getSourceNodeId()) && continueNodeId.equals(edge.getTargetNodeId())) {
                continue;
            }
            mergedEdges.add(edge);
        }

        for (int index = 0; index < toolCalls.size(); index++) {
            ParsedToolCall call = toolCalls.get(index);
            String nodeId = prefix + "step-" + index;
            dynamicNodeIds.add(nodeId);
            Map<String, Object> configuration = new LinkedHashMap<>();
            configuration.put("toolId", call.toolId());
            if (!call.arguments().isEmpty()) {
                configuration.put("arguments", call.arguments());
            }
            configuration.put(ToolCallPlannerSupport.CONFIG_AGGREGATE_TOOL_RESULT, true);
            builder.addNode(withDefaultPorts(NodeDefinition.builder()
                    .id(nodeId)
                    .type(NodeType.TOOL.name())
                    .label(DynamicNodeLabels.prefixed(ToolLabelResolver.resolve(call.toolId(), graph)))
                    .configuration(configuration)
                    .build()));
        }

        String synthesisNodeId = prefix + ToolCallPlannerSupport.DEFAULT_SYNTHESIS_NODE_SUFFIX;
        builder.addNode(withDefaultPorts(NodeDefinition.builder()
                .id(synthesisNodeId)
                .type(NodeType.AGENT.name())
                .label(DynamicNodeLabels.prefixed("Tool synthesis"))
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .putConfiguration("promptTemplate", ToolCallPlannerSupport.TOOL_SYNTHESIS_PROMPT_TEMPLATE)
                .putConfiguration(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS, true)
                .build()));

        String entryNodeId = dynamicNodeIds.getFirst();
        mergedEdges.add(EdgeDefinition.builder()
                .sourceNodeId(plannerNodeId)
                .sourcePortId("out")
                .targetNodeId(entryNodeId)
                .targetPortId("in")
                .build());

        for (int index = 0; index < dynamicNodeIds.size(); index++) {
            String current = dynamicNodeIds.get(index);
            String next = index + 1 < dynamicNodeIds.size() ? dynamicNodeIds.get(index + 1) : synthesisNodeId;
            mergedEdges.add(EdgeDefinition.builder()
                    .sourceNodeId(current)
                    .sourcePortId("out")
                    .targetNodeId(next)
                    .targetPortId("in")
                    .build());
        }

        mergedEdges.add(EdgeDefinition.builder()
                .sourceNodeId(synthesisNodeId)
                .sourcePortId("out")
                .targetNodeId(continueNodeId)
                .targetPortId("in")
                .build());

        builder.replaceEdges(mergedEdges);
        return new MergeResult(builder.build(), entryNodeId);
    }

    public static int readRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        Object raw = variables.get(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    public static void incrementRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE, readRetryCount(variables) + 1);
    }

    public static void resetRetryCount(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE, 0);
    }

    public static void resetToolResults(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE, "[]");
    }

    static Set<String> parseAllowedToolIds(String allowedToolsJson) {
        Set<String> allowed = new LinkedHashSet<>();
        if (allowedToolsJson == null || allowedToolsJson.isBlank()) {
            return allowed;
        }
        try {
            JsonNode root = MAPPER.readTree(allowedToolsJson.trim());
            if (!root.isArray()) {
                return allowed;
            }
            for (JsonNode entry : root) {
                if (entry.isObject()) {
                    String toolId = text(entry, "toolId");
                    if (toolId != null && !toolId.isBlank()) {
                        allowed.add(toolId);
                    }
                } else if (entry.isTextual()) {
                    allowed.add(entry.asText());
                }
            }
        } catch (Exception ignored) {
            return allowed;
        }
        return allowed;
    }

    private static NodeDefinition withDefaultPorts(NodeDefinition node) {
        if (node.getPorts() != null && !node.getPorts().isEmpty()) {
            return node;
        }
        NodeDefinition.Builder builder = NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(node.getLabel())
                .execution(node.getExecution())
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

    public record ParsedToolCall(String toolId, Map<String, Object> arguments) {
    }

    public record ValidationResult(
            boolean valid,
            Kind kind,
            String normalizedJson,
            String directResponse,
            List<ParsedToolCall> toolCalls,
            String message) {

        public enum Kind {
            TOOL_CALLS,
            DIRECT_RESPONSE,
            INVALID
        }

        public static ValidationResult toolCalls(String normalizedJson, List<ParsedToolCall> toolCalls) {
            return new ValidationResult(true, Kind.TOOL_CALLS, normalizedJson, null, List.copyOf(toolCalls), null);
        }

        public static ValidationResult directResponse(String normalizedJson, String directResponse) {
            return new ValidationResult(
                    true, Kind.DIRECT_RESPONSE, normalizedJson, directResponse, List.of(), null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, Kind.INVALID, null, null, List.of(), message);
        }
    }

    public record MergeResult(WorkflowDefinition graph, String entryNodeId) {
    }
}
