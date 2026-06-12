package org.olo.kernel.agent.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.agent.LlmInvocationService;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.model.ModelProviderResolver;
import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.agent.prompt.PromptRenderer;

import java.util.Objects;

public final class DefaultLlmInvocationService implements LlmInvocationService {

    private final PromptRenderer promptRenderer;
    private final ModelProviderResolver modelProviderResolver;
    private final LlmClient llmClient;

    public DefaultLlmInvocationService(
            PromptRenderer promptRenderer,
            ModelProviderResolver modelProviderResolver,
            LlmClient llmClient) {
        this.promptRenderer = Objects.requireNonNull(promptRenderer, "promptRenderer");
        this.modelProviderResolver = Objects.requireNonNull(modelProviderResolver, "modelProviderResolver");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    @Override
    public LlmInvocationResult invoke(KernelRuntimeContext context) {
        Objects.requireNonNull(context, "context");
        ResolvedModelCall modelCall = modelProviderResolver.resolve(context.getGraph());
        String renderedPrompt = promptRenderer.render(context.getGraph(), context.getVariables());
        String response = llmClient.complete(modelCall, renderedPrompt);
        return new LlmInvocationResult(modelCall, renderedPrompt, response);
    }
}
