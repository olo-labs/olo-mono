/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.model;

import java.util.List;

public record ToolCallValidationResult(
        boolean valid,
        Kind kind,
        String normalizedJson,
        String directResponse,
        List<ParsedToolCall> toolCalls,
        List<ParsedAgentCall> agentCalls,
        String message) {

    public enum Kind {
        TOOL_CALLS,
        DIRECT_RESPONSE,
        INVALID
    }

    public static ToolCallValidationResult calls(
            String normalizedJson, List<ParsedToolCall> toolCalls, List<ParsedAgentCall> agentCalls) {
        return new ToolCallValidationResult(
                true,
                Kind.TOOL_CALLS,
                normalizedJson,
                null,
                List.copyOf(toolCalls),
                List.copyOf(agentCalls),
                null);
    }

    public static ToolCallValidationResult directResponse(String normalizedJson, String directResponse) {
        return new ToolCallValidationResult(
                true, Kind.DIRECT_RESPONSE, normalizedJson, directResponse, List.of(), List.of(), null);
    }

    public static ToolCallValidationResult invalid(String message) {
        return new ToolCallValidationResult(false, Kind.INVALID, null, null, List.of(), List.of(), message);
    }
}
