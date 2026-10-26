/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agentcall.model;

import java.util.List;

public record AgentCallValidationResult(
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

    public static AgentCallValidationResult calls(
            String normalizedJson, List<ParsedToolCall> toolCalls, List<ParsedAgentCall> agentCalls) {
        return new AgentCallValidationResult(
                true,
                Kind.TOOL_CALLS,
                normalizedJson,
                null,
                List.copyOf(toolCalls),
                List.copyOf(agentCalls),
                null);
    }

    public static AgentCallValidationResult directResponse(String normalizedJson, String directResponse) {
        return new AgentCallValidationResult(
                true, Kind.DIRECT_RESPONSE, normalizedJson, directResponse, List.of(), List.of(), null);
    }

    public static AgentCallValidationResult invalid(String message) {
        return new AgentCallValidationResult(false, Kind.INVALID, null, null, List.of(), List.of(), message);
    }
}
