/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.traversal.step.handler.impl.ToolNodeTypeHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds {@code availableToolsJson} from capabilities edges and workflow tool artifacts.
 */
public final class AvailableToolsJsonResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AvailableToolsJsonResolver() {
    }

    public static String resolve(WorkflowDefinition graph, String plannerNodeId) {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, ToolDefinition> toolsByNodeId = indexToolsByCanvasNode(graph);
        for (EdgeDefinition edge : graph.getEdges()) {
            if (!plannerNodeId.equals(edge.getTargetNodeId()) || !"capabilities".equals(edge.getTargetPortId())) {
                continue;
            }
            String sourceNodeId = edge.getSourceNodeId();
            Optional<NodeDefinition> sourceNode = graph.getNodes().stream()
                    .filter(node -> sourceNodeId.equals(node.getId()))
                    .findFirst();
            if (sourceNode.isEmpty() || !NodeType.TOOL.name().equals(sourceNode.get().getType())) {
                continue;
            }
            ToolDefinition tool = toolsByNodeId.get(sourceNodeId);
            if (tool == null) {
                tool = toolsByNodeId.get(readConfiguredToolId(sourceNode.get()));
            }
            if (tool == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            String implementationId = tool.getRuntimeBinding() == null
                    ? null
                    : tool.getRuntimeBinding().getImplementationId();
            String toolId = implementationId != null && !implementationId.isBlank()
                    ? implementationId
                    : ToolNodeTypeHandler.resolveToolId(readConfiguredToolId(sourceNode.get()));
            entry.put("toolId", toolId);
            if (tool.getCapability() != null) {
                entry.put("name", tool.getCapability().getName());
                entry.put("description", tool.getCapability().getDescription());
            }
            tools.add(entry);
        }
        try {
            String json = MAPPER.writeValueAsString(tools);
            return filterToolsByPlannerContext(graph, json);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String filterToolsByPlannerContext(WorkflowDefinition graph, String json) {
        if (!PlannerContextRuntime.injectCapabilities(graph)) {
            return "[]";
        }
        return PlannerContextRuntime.selectedToolIds(graph)
                .map(selected -> filterToolJson(json, selected))
                .orElse(json);
    }

    private static String filterToolJson(String json, Set<String> selectedToolIds) {
        if (selectedToolIds.isEmpty()) {
            return "[]";
        }
        try {
            var root = MAPPER.readTree(json);
            if (!root.isArray()) {
                return "[]";
            }
            var filtered = MAPPER.createArrayNode();
            for (var entry : root) {
                if (!entry.isObject()) {
                    continue;
                }
                String toolId = entry.has("toolId") ? entry.get("toolId").asText() : null;
                if (toolId != null && selectedToolIds.contains(toolId)) {
                    filtered.add(entry);
                }
            }
            return MAPPER.writeValueAsString(filtered);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static void appendToolResult(
            org.olo.kernel.context.variables.WorkflowRuntimeVariables variables,
            String toolId,
            String nodeId,
            boolean success,
            String response) {
        appendToolResult(variables, toolId, nodeId, success, response, Map.of());
    }

    public static void appendToolResult(
            org.olo.kernel.context.variables.WorkflowRuntimeVariables variables,
            String toolId,
            String nodeId,
            boolean success,
            String response,
            Map<String, Object> output) {
        try {
            ArrayNode results = readResultsArray(variables);
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("toolId", toolId);
            entry.put("nodeId", nodeId);
            entry.put("success", success);
            entry.put("response", response == null ? "" : response);
            if (output != null && !output.isEmpty()) {
                entry.set("output", MAPPER.valueToTree(output));
            }
            results.add(entry);
            variables.set(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE, MAPPER.writeValueAsString(results));
        } catch (Exception ignored) {
            variables.set(
                    ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE,
                    "[{\"toolId\":\"" + toolId + "\",\"success\":" + success + "}]");
        }
    }

    private static ArrayNode readResultsArray(org.olo.kernel.context.variables.WorkflowRuntimeVariables variables)
            throws Exception {
        String raw = variables.getString(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE);
        if (raw == null || raw.isBlank()) {
            return MAPPER.createArrayNode();
        }
        return (ArrayNode) MAPPER.readTree(raw);
    }

    private static Map<String, ToolDefinition> indexToolsByCanvasNode(WorkflowDefinition graph) {
        Map<String, ToolDefinition> indexed = new LinkedHashMap<>();
        if (graph.getTools() == null) {
            return indexed;
        }
        for (ToolDefinition tool : graph.getTools()) {
            indexed.put(tool.getId(), tool);
            if (tool.getRuntimeBinding() != null && tool.getRuntimeBinding().getImplementationId() != null) {
                indexed.put(tool.getRuntimeBinding().getImplementationId(), tool);
            }
        }
        return indexed;
    }

    private static String readConfiguredToolId(NodeDefinition node) {
        if (node.getConfiguration() == null) {
            return null;
        }
        Object value = node.getConfiguration().get("toolId");
        return value == null ? null : String.valueOf(value);
    }
}
