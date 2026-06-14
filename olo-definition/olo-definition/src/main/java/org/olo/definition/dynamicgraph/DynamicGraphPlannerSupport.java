package org.olo.definition.dynamicgraph;

import org.olo.definition.node.NodeDefinition;

import java.util.Map;

/**
 * Shared configuration keys for inline dynamic graph planner nodes.
 */
public final class DynamicGraphPlannerSupport {

    public static final String CONFIG_DYNAMIC_GRAPH_PLANNER = "dynamicGraphPlanner";
    public static final String CONFIG_OUTPUT_VARIABLE = "outputVariable";
    public static final String CONFIG_MAX_INVALID_JSON_RETRIES = "maxInvalidJsonRetries";
    public static final String CONFIG_CONTINUE_NODE_ID = "continueNodeId";

    public static final String DEFAULT_OUTPUT_VARIABLE = "generatedGraphJson";
    public static final String DEFAULT_RETRY_VARIABLE = "generatedGraphJsonRetryCount";
    public static final String DEFAULT_VALIDATION_ERROR_VARIABLE = "generatedGraphJsonValidationError";
    public static final String DEFAULT_PLANNER_NODE_ID = "graph-planner";
    public static final int DEFAULT_MAX_INVALID_JSON_RETRIES = 3;

    private DynamicGraphPlannerSupport() {
    }

    public static boolean isDynamicGraphPlanner(NodeDefinition node) {
        return node != null
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(CONFIG_DYNAMIC_GRAPH_PLANNER));
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
