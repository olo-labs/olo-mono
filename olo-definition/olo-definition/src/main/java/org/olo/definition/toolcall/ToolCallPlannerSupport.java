package org.olo.definition.toolcall;

import org.olo.definition.node.NodeDefinition;

import java.util.Map;

/**
 * Shared configuration keys for inline tool-call planner agent nodes and dynamic tool execution.
 */
public final class ToolCallPlannerSupport {

    public static final String CONFIG_TOOL_CALL_PLANNER = "toolCallPlanner";
    public static final String CONFIG_OUTPUT_VARIABLE = "outputVariable";
    public static final String CONFIG_MAX_INVALID_JSON_RETRIES = "maxInvalidJsonRetries";
    public static final String CONFIG_CONTINUE_NODE_ID = "continueNodeId";
    public static final String CONFIG_AGGREGATE_TOOL_RESULT = "aggregateToolResult";

    public static final String METADATA_DYNAMIC_TOOL_EXECUTION = "dynamicToolExecution";
    public static final String METADATA_PLANNER_NODE_ID = "plannerNodeId";
    public static final String METADATA_AVAILABLE_TOOLS_VARIABLE = "availableToolsVariable";

    public static final String DEFAULT_PLANNER_NODE_ID = "agent";
    public static final String DEFAULT_OUTPUT_VARIABLE = "toolCallSequenceJson";
    public static final String DEFAULT_AVAILABLE_TOOLS_VARIABLE = "availableToolsJson";
    public static final String DEFAULT_TOOL_RESULTS_VARIABLE = "toolResultsJson";
    public static final String DEFAULT_RETRY_VARIABLE = "toolCallSequenceJsonRetryCount";
    public static final String DEFAULT_VALIDATION_ERROR_VARIABLE = "toolCallSequenceJsonValidationError";
    public static final String DEFAULT_SYNTHESIS_NODE_SUFFIX = "tool-synthesis";

    public static final int DEFAULT_MAX_INVALID_JSON_RETRIES = 3;

    public static final String TOOL_SYNTHESIS_PROMPT_TEMPLATE =
            """
            You are an OLO agent synthesizing a final answer from tool results.

            User request:
            {message}

            Tool results (JSON):
            {toolResultsJson}

            Respond clearly and actionably using the tool results. If a tool failed or returned no data, say so.""";

    private ToolCallPlannerSupport() {
    }

    public static boolean isToolCallPlanner(NodeDefinition node) {
        return node != null
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(CONFIG_TOOL_CALL_PLANNER));
    }

    public static String outputVariable(NodeDefinition node) {
        return stringConfig(node, CONFIG_OUTPUT_VARIABLE, DEFAULT_OUTPUT_VARIABLE);
    }

    public static int maxInvalidJsonRetries(NodeDefinition node) {
        Map<String, Object> configuration = node != null ? node.getConfiguration() : null;
        if (configuration == null) {
            return DEFAULT_MAX_INVALID_JSON_RETRIES;
        }
        Object raw = configuration.get(CONFIG_MAX_INVALID_JSON_RETRIES);
        if (raw == null) {
            return DEFAULT_MAX_INVALID_JSON_RETRIES;
        }
        int value = Integer.parseInt(String.valueOf(raw));
        return Math.max(value, 1);
    }

    public static String continueNodeId(NodeDefinition node, String defaultNodeId) {
        return stringConfig(node, CONFIG_CONTINUE_NODE_ID, defaultNodeId);
    }

    private static String stringConfig(NodeDefinition node, String key, String defaultValue) {
        if (node == null || node.getConfiguration() == null) {
            return defaultValue;
        }
        Object value = node.getConfiguration().get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }
}
