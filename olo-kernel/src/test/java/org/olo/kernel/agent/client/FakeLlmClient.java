package org.olo.kernel.agent.client;

import org.olo.kernel.agent.model.ResolvedModelCall;

public final class FakeLlmClient implements LlmClient {

    @Override
    public String complete(ResolvedModelCall modelCall, String prompt) {
        if (prompt != null && prompt.contains("tool-call planner")) {
            return """
                    {"toolCalls":[],"directResponse":"LLM response for: tool-call planner"}
                    """;
        }
        return "LLM response for: " + prompt;
    }
}
