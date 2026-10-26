/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agentcall.model;
import java.util.List;

public record AgentCallValidationResult(boolean valid, Kind kind, String normalizedJson,
        String directResponse, List<ParsedAgentCall> agentCalls, String message) {
    public enum Kind { AGENT_CALLS, DIRECT_RESPONSE, INVALID }
    public static AgentCallValidationResult calls(String json, List<ParsedAgentCall> calls) {
        return new AgentCallValidationResult(true, Kind.AGENT_CALLS, json, null, List.copyOf(calls), null);
    }
    public static AgentCallValidationResult directResponse(String json, String response) {
        return new AgentCallValidationResult(true, Kind.DIRECT_RESPONSE, json, response, List.of(), null);
    }
    public static AgentCallValidationResult invalid(String reason) {
        return new AgentCallValidationResult(false, Kind.INVALID, null, null, List.of(), reason);
    }
}
