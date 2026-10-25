/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.exception.KernelException;
import org.olo.toolgate.client.AgentNames;

import java.util.Map;
import java.util.Objects;

/**
 * Per-AGENT-node ToolGate settings read from the node's {@code configuration}.
 *
 * <ul>
 *   <li>{@code agentName}: the ToolGate identity whose catalog this agent uses. Defaults to the agent's own name:
 *       the workflow id for a workflow's primary agent (node id {@code agent}, or the only AGENT node), otherwise
 *       the node id.</li>
 *   <li>{@code maxToolRounds}: tool rounds per turn; {@code 0} turns tools off for this agent.</li>
 * </ul>
 *
 * @param agentName     resolved ToolGate identity
 * @param maxToolRounds resolved round limit
 */
public record AgentToolSettings(String agentName, int maxToolRounds) {

    public static final String CONFIG_AGENT_NAME = "agentName";
    public static final String CONFIG_MAX_TOOL_ROUNDS = "maxToolRounds";
    static final String PRIMARY_AGENT_NODE_ID = "agent";

    public static AgentToolSettings resolve(WorkflowDefinition graph, NodeDefinition node, int defaultMaxToolRounds) {
        Objects.requireNonNull(node, "node");
        Map<String, Object> configuration = node.getConfiguration() == null ? Map.of() : node.getConfiguration();
        return new AgentToolSettings(
                resolveAgentName(graph, node, configuration.get(CONFIG_AGENT_NAME)),
                resolveMaxToolRounds(node, configuration.get(CONFIG_MAX_TOOL_ROUNDS), defaultMaxToolRounds));
    }

    /**
     * Planner agents answer with a JSON plan, not prose, so they never get tools. They are removed in M2/M3.
     */
    public static boolean usesTools(NodeDefinition node) {
        if (DynamicGraphPlannerSupport.isDynamicGraphPlanner(node) || ToolCallPlannerSupport.isToolCallPlanner(node)) {
            return false;
        }
        var configuration = node.getConfiguration() == null ? Map.<String, Object>of() : node.getConfiguration();
        return resolveMaxToolRounds(node, configuration.get(CONFIG_MAX_TOOL_ROUNDS), 1) != 0;
    }

    static String defaultAgentName(WorkflowDefinition graph, NodeDefinition node) {
        String workflowId = graph != null ? graph.getId() : null;
        boolean primary = PRIMARY_AGENT_NODE_ID.equals(node.getId()) || onlyAgentNode(graph, node);
        if (primary && workflowId != null && !workflowId.isBlank()) {
            return workflowId;
        }
        return node.getId();
    }

    private static String resolveAgentName(WorkflowDefinition graph, NodeDefinition node, Object configured) {
        String name = configured instanceof String text && !text.isBlank()
                ? text.trim()
                : defaultAgentName(graph, node);
        if (!AgentNames.isValid(name)) {
            throw new KernelException("AGENT node '" + node.getId() + "' has invalid agentName '" + name
                    + "': use 1-128 letters, digits, '.', '_' or '-', starting with a letter or digit");
        }
        return name;
    }

    private static int resolveMaxToolRounds(NodeDefinition node, Object configured, int defaultMaxToolRounds) {
        if (configured == null) {
            return defaultMaxToolRounds;
        }
        int rounds;
        if (configured instanceof Number number) {
            rounds = number.intValue();
        } else {
            try {
                rounds = Integer.parseInt(configured.toString().trim());
            } catch (NumberFormatException e) {
                throw new KernelException("AGENT node '" + node.getId() + "' has non-numeric maxToolRounds: " + configured);
            }
        }
        if (rounds < 0) {
            throw new KernelException("AGENT node '" + node.getId() + "' has negative maxToolRounds: " + rounds);
        }
        return rounds;
    }

    private static boolean onlyAgentNode(WorkflowDefinition graph, NodeDefinition node) {
        if (graph == null || graph.getNodes() == null) {
            return false;
        }
        long agents = graph.getNodes().stream().filter(n -> NodeType.AGENT.name().equals(n.getType())).count();
        return agents == 1 && NodeType.AGENT.name().equals(node.getType());
    }
}
