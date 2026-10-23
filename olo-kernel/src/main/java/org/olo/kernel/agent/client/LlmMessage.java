/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client;

import java.util.List;
import java.util.Objects;

/**
 * One chat message in a tool-calling conversation.
 *
 * @param role      {@code user}, {@code assistant} or {@code tool}
 * @param content   text content (may be empty for an assistant turn that only calls tools)
 * @param toolCalls tool calls requested by an assistant turn
 * @param toolName  for {@code tool} messages: the (model-facing) name of the tool whose result this is
 */
public record LlmMessage(String role, String content, List<LlmToolCall> toolCalls, String toolName) {

    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String TOOL = "tool";

    public LlmMessage {
        Objects.requireNonNull(role, "role");
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(USER, content, List.of(), null);
    }

    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage(ASSISTANT, content, toolCalls, null);
    }

    public static LlmMessage toolResult(String toolName, String content) {
        return new LlmMessage(TOOL, content, List.of(), toolName);
    }
}
