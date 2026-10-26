/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.agentcall;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.node.NodeDefinition;

import java.util.Map;

/**
 * Shared configuration keys for agent delegation and child-result synthesis.
 */
public final class AgentCallPlannerSupport {

    public static final String CONFIG_AGENT_CALL_PLANNER = "agentCallPlanner";
    public static final String CONFIG_OUTPUT_VARIABLE = "outputVariable";
    public static final String CONFIG_MAX_INVALID_JSON_RETRIES = "maxInvalidJsonRetries";
    public static final String CONFIG_CONTINUE_NODE_ID = "continueNodeId";

    public static final String METADATA_PLANNER_NODE_ID = "plannerNodeId";

    public static final String DEFAULT_PLANNER_NODE_ID = "agent";
    public static final String DEFAULT_OUTPUT_VARIABLE = "agentCallSequenceJson";
    public static final String DEFAULT_AVAILABLE_AGENTS_VARIABLE = "availableAgentsJson";
    public static final String METADATA_AVAILABLE_AGENTS_VARIABLE = "availableAgentsVariable";
    public static final String DEFAULT_AGENT_RESULTS_VARIABLE = "agentResultsJson";
    public static final String DEFAULT_RETRY_VARIABLE = "agentCallSequenceJsonRetryCount";
    public static final String DEFAULT_VALIDATION_ERROR_VARIABLE = "agentCallSequenceJsonValidationError";
    public static final String DEFAULT_AGENT_SYNTHESIS_NODE_SUFFIX = "agent-synthesis";

    public static final int DEFAULT_MAX_INVALID_JSON_RETRIES = 3;

    public static final String AGENT_SYNTHESIS_PROMPT_TEMPLATE =
            "You are an "
                    + OloProductTerminology.PRODUCT
                    + """
                     agent synthesizing a final answer from child agent workflow results.

                    User request:
                    {message}

                    Child agent results (JSON):
                    {agentResultsJson}

                    Respond clearly and actionably using the child agent results. If a child agent failed or returned no data, say so.""";

    private AgentCallPlannerSupport() {
    }

    public static boolean isAgentCallPlanner(NodeDefinition node) {
        return node != null
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(CONFIG_AGENT_CALL_PLANNER));
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
