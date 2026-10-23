/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client;

import java.util.List;

/**
 * The model's reply in a tool-calling conversation: either tool calls to run, or a final answer.
 */
public record LlmChatResponse(String content, List<LlmToolCall> toolCalls) {

    public LlmChatResponse {
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
