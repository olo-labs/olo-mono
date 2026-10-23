/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.agent.LlmInvocationService;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.model.ModelProviderResolver;
import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.agent.prompt.PromptRenderer;
import org.olo.kernel.agent.tool.AgentToolAccess;
import org.olo.kernel.agent.tool.AgentToolLoop;
import org.olo.kernel.agent.tool.AgentToolLoopResult;
import org.olo.kernel.agent.tool.AgentToolSettings;

import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultLlmInvocationService implements LlmInvocationService {

    private final PromptRenderer promptRenderer;
    private final ModelProviderResolver modelProviderResolver;
    private final LlmClient llmClient;
    private final Supplier<AgentToolAccess> toolAccess;
    private final AgentToolLoop toolLoop;

    public DefaultLlmInvocationService(
            PromptRenderer promptRenderer,
            ModelProviderResolver modelProviderResolver,
            LlmClient llmClient) {
        this(promptRenderer, modelProviderResolver, llmClient, AgentToolAccess::disabled);
    }

    /**
     * @param toolAccess read on every invocation, so the worker can install ToolGate access after the engine is built
     */
    public DefaultLlmInvocationService(
            PromptRenderer promptRenderer,
            ModelProviderResolver modelProviderResolver,
            LlmClient llmClient,
            Supplier<AgentToolAccess> toolAccess) {
        this.promptRenderer = Objects.requireNonNull(promptRenderer, "promptRenderer");
        this.modelProviderResolver = Objects.requireNonNull(modelProviderResolver, "modelProviderResolver");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.toolAccess = Objects.requireNonNull(toolAccess, "toolAccess");
        this.toolLoop = new AgentToolLoop(llmClient);
    }

    @Override
    public LlmInvocationResult invoke(KernelRuntimeContext context, NodeDefinition node) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(node, "node");
        ResolvedModelCall modelCall = modelProviderResolver.resolve(context.getGraph());
        String renderedPrompt = promptRenderer.renderForNode(context.getGraph(), node, context.getVariables());

        AgentToolAccess access = toolAccess.get();
        if (access == null || !access.isEnabled() || !AgentToolSettings.usesTools(node)) {
            String response = llmClient.complete(modelCall, renderedPrompt);
            return new LlmInvocationResult(modelCall, renderedPrompt, response);
        }

        AgentToolSettings settings = AgentToolSettings.resolve(context.getGraph(), node, access.defaultMaxToolRounds());
        // M1-R2: only operator-mapped identities; never whatever name the workflow author typed.
        access.identityPolicy().authorize(tenantId(context), context.getGraph().getId(), settings.agentName());
        AgentToolLoopResult result = toolLoop.run(context, node, modelCall, renderedPrompt, access.client(), settings);
        return new LlmInvocationResult(modelCall, renderedPrompt, result.response(), result);
    }

    /**
     * The run's tenant. Until M3.3 delivers a verified identity context, this is the tenant in the workflow input,
     * so mappings scoped to a specific tenant are only as trustworthy as that input.
     */
    private static String tenantId(KernelRuntimeContext context) {
        if (context.getInput() == null || context.getInput().getContext() == null) {
            return null;
        }
        return context.getInput().getContext().getTenantId();
    }
}
