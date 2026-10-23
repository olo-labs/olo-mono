/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import java.util.List;
import java.util.Map;

/**
 * Result of one agent turn run through {@link AgentToolLoop}.
 *
 * @param response    the agent's answer (or the waiting-for-approval notice)
 * @param outcome     how the turn ended
 * @param agentName   the ToolGate identity used
 * @param catalog     ToolGate tool names the agent was offered
 * @param invocations every tool call, in order
 */
public record AgentToolLoopResult(
        String response,
        AgentToolOutcome outcome,
        String agentName,
        List<String> catalog,
        List<ToolInvocationRecord> invocations) {

    public AgentToolLoopResult {
        catalog = catalog == null ? List.of() : List.copyOf(catalog);
        invocations = invocations == null ? List.of() : List.copyOf(invocations);
    }

    public List<Map<String, Object>> invocationOutputs() {
        return invocations.stream().map(ToolInvocationRecord::toOutput).toList();
    }
}
