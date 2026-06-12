package org.olo.kernel.agent.client;

import org.olo.kernel.agent.model.ResolvedModelCall;

public final class FakeLlmClient implements LlmClient {

    @Override
    public String complete(ResolvedModelCall modelCall, String prompt) {
        return "LLM response for: " + prompt;
    }
}
