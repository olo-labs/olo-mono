package org.olo.kernel.agent.client;

import org.olo.kernel.agent.model.ResolvedModelCall;

/**
 * Executes a single LLM completion for a rendered prompt.
 */
public interface LlmClient {

    String complete(ResolvedModelCall modelCall, String prompt);
}
