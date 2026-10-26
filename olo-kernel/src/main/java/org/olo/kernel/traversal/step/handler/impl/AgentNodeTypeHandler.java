/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.agentcall.AgentCallPlannerSupport;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.agent.executor.AgentExecutorRegistry;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.agentcall.AvailableAgentsJsonResolver;
import org.olo.kernel.agentcall.AvailableToolsJsonResolver;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.spi.node.NodeResult;

import java.util.Objects;

/**
 * Executes {@code AGENT} canvas nodes by delegating to an {@link AgentExecutor}.
 */
public final class AgentNodeTypeHandler implements NodeTypeHandler {

    private final AgentExecutorRegistry agentExecutorRegistry;

    public AgentNodeTypeHandler(AgentExecutorRegistry agentExecutorRegistry) {
        this.agentExecutorRegistry = Objects.requireNonNull(agentExecutorRegistry, "agentExecutorRegistry");
    }

    @Override
    public boolean supports(String nodeType) {
        return NodeType.AGENT.name().equals(nodeType);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        if (AgentCallPlannerSupport.isToolCallPlanner(node) && context.getGraph() != null) {
            String availableToolsJson =
                    AvailableToolsJsonResolver.resolve(context.getGraph(), node.getId());
            context.getVariables()
                    .set(AgentCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE, availableToolsJson);
            String availableAgentsJson =
                    AvailableAgentsJsonResolver.resolve(context.getGraph(), node.getId());
            context.getVariables()
                    .set(AgentCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE, availableAgentsJson);
            if (context.getVariables().getString(AgentCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE) == null) {
                context.getVariables().set(AgentCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE, "[]");
            }
        }
        AgentExecutor executor = agentExecutorRegistry.resolve(context, node);
        TraversalDiagnostics.logAgentExecutorSelected(node.getId(), executor.id());
        return executor.execute(context, node);
    }
}
