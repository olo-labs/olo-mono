/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client;

import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.exception.KernelException;

import java.util.List;

/**
 * Executes a single LLM completion for a rendered prompt.
 */
public interface LlmClient {

    String complete(ResolvedModelCall modelCall, String prompt);

    /**
     * One turn of a tool-calling conversation. {@code tools} may be empty, which asks for a final answer.
     */
    default LlmChatResponse chat(ResolvedModelCall modelCall, List<LlmMessage> messages, List<LlmToolSpec> tools) {
        throw new KernelException("LLM client " + getClass().getSimpleName() + " does not support tool calling");
    }
}
