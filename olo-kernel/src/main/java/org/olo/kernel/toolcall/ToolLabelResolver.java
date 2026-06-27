package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Resolves human-readable tool labels for dynamically injected TOOL nodes.
 */
public final class ToolLabelResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolLabelResolver() {
    }

    public static String resolve(String toolId, WorkflowDefinition graph, String availableToolsJson) {
        if (toolId == null || toolId.isBlank()) {
            return "Tool";
        }
        String fromGraph = resolveFromWorkflowTools(toolId, graph);
        if (fromGraph != null) {
            return fromGraph;
        }
        String fromAvailableTools = resolveFromAvailableToolsJson(toolId, availableToolsJson);
        if (fromAvailableTools != null) {
            return fromAvailableTools;
        }
        int colon = toolId.lastIndexOf(':');
        return colon >= 0 ? toolId.substring(colon + 1) : toolId;
    }

    public static String resolve(String toolId, WorkflowDefinition graph) {
        return resolve(toolId, graph, null);
    }

    static String resolveFromWorkflowTools(String toolId, WorkflowDefinition graph) {
        if (graph == null || graph.getTools() == null) {
            return null;
        }
        for (ToolDefinition tool : graph.getTools()) {
            if (!matchesToolId(tool, toolId)) {
                continue;
            }
            if (tool.getCapability() != null
                    && tool.getCapability().getName() != null
                    && !tool.getCapability().getName().isBlank()) {
                return tool.getCapability().getName().trim();
            }
        }
        return null;
    }

    static String resolveFromAvailableToolsJson(String toolId, String availableToolsJson) {
        if (availableToolsJson == null || availableToolsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(availableToolsJson.trim());
            if (!root.isArray()) {
                return null;
            }
            for (JsonNode entry : root) {
                if (!entry.isObject()) {
                    continue;
                }
                JsonNode entryToolId = entry.get("toolId");
                if (entryToolId == null || !entryToolId.isTextual() || !toolId.equals(entryToolId.asText())) {
                    continue;
                }
                JsonNode name = entry.get("name");
                if (name != null && name.isTextual() && !name.asText().isBlank()) {
                    return name.asText().trim();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static boolean matchesToolId(ToolDefinition tool, String toolId) {
        if (toolId.equals(tool.getId())) {
            return true;
        }
        return tool.getRuntimeBinding() != null
                && toolId.equals(tool.getRuntimeBinding().getImplementationId());
    }
}
