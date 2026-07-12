/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.executor.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.agent.LlmInvocationService;
import org.olo.kernel.agent.executor.AgentExecutor;
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
            TraversalDiagnostics.logLlmInvocation(node.getId(), invocation);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("response", invocation.response());
            output.put("renderedPrompt", invocation.renderedPrompt());
            output.put("model", invocation.modelCall().model());
            output.put("providerId", invocation.modelCall().providerId());
            output.put("agentExecutor", EXECUTOR_ID);
            output.put("agentMode", EXECUTOR_ID);

            context.getVariables().set("agentStatus", EXECUTOR_ID);
            if (DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)) {
                context.getVariables().set(
                        DynamicGraphPlannerSupport.outputVariable(node), invocation.response());
            }
            if (ToolCallPlannerSupport.isToolCallPlanner(node)) {
                context.getVariables().set(
                        ToolCallPlannerSupport.outputVariable(node), invocation.response());
            }
            return NodeResult.completed(invocation.response(), output);
        } catch (RuntimeException e) {
            TraversalDiagnostics.logLlmFailure(node.getId(), e.getMessage());
            return NodeResult.failed(e.getMessage(), e);
        }
    }
}
