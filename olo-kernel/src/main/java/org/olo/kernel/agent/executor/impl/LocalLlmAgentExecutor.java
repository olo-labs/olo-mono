/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.executor.impl;

import org.olo.definition.agentcall.AgentCallPlannerSupport;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.agent.LlmInvocationService;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.agent.tool.AgentToolLoopResult;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.spi.node.NodeResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Inline agent: workflow default prompt + routed model provider (Ollama-compatible client).
 */
public final class LocalLlmAgentExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "local-llm";

    private final LlmInvocationService llmInvocationService;

    public LocalLlmAgentExecutor(LlmInvocationService llmInvocationService) {
        this.llmInvocationService = Objects.requireNonNull(llmInvocationService, "llmInvocationService");
    }

    @Override
    public String id() {
        return EXECUTOR_ID;
    }

    @Override
    public boolean supports(NodeDefinition node) {
        return node != null && NodeType.AGENT.name().equals(node.getType());
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        try {
            LlmInvocationResult invocation = llmInvocationService.invoke(context, node);
            if (invocation.isContinuing()) {
                return NodeResult.continuing(invocation.progress());
            }
            TraversalDiagnostics.logLlmInvocation(node.getId(), invocation);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("response", invocation.response());
            output.put("renderedPrompt", invocation.renderedPrompt());
            output.put("model", invocation.modelCall().model());
            output.put("providerId", invocation.modelCall().providerId());
            output.put("agentExecutor", EXECUTOR_ID);
            output.put("agentMode", EXECUTOR_ID);

            AgentToolLoopResult toolLoop = invocation.toolLoop();
            if (toolLoop != null && (toolLoop.outcome() == org.olo.kernel.agent.tool.AgentToolOutcome.OUTCOME_UNKNOWN
                    || toolLoop.outcome() == org.olo.kernel.agent.tool.AgentToolOutcome.APPROVAL_EXPIRED
                    || toolLoop.outcome() == org.olo.kernel.agent.tool.AgentToolOutcome.INVOCATION_ENDED)) {
                // M5.4/M5.5: the run must not carry on as if the call had finished; reconciliation or a new request decides.
                return NodeResult.failed(toolLoop.response(), null);
            }
            if (toolLoop != null) {
                output.put("agentName", toolLoop.agentName());
                output.put("toolCatalog", toolLoop.catalog());
                output.put("toolInvocations", toolLoop.invocationOutputs());
                output.put("agentOutcome", toolLoop.outcome().name());
            }

            context.getVariables().set("agentStatus", EXECUTOR_ID);
            if (DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)) {
                context.getVariables().set(
                        DynamicGraphPlannerSupport.outputVariable(node), invocation.response());
            }
            if (AgentCallPlannerSupport.isAgentCallPlanner(node)) {
                context.getVariables().set(
                        AgentCallPlannerSupport.outputVariable(node), invocation.response());
            }
            return NodeResult.completed(invocation.response(), output);
        } catch (RuntimeException e) {
            if(e instanceof org.olo.kernel.knowledge.KnowledgeRuntime.CheckpointExpired || e instanceof org.olo.toolgate.client.GovernedCalls.Pending)throw e;
            TraversalDiagnostics.logLlmFailure(node.getId(), e.getMessage());
            return NodeResult.failed(e.getMessage(), e);
        }
    }
}
