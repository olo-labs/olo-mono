/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent;

import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.agent.tool.AgentToolLoopResult;

/**
 * Outcome of rendering a workflow prompt and invoking the configured model provider.
 *
 * @param toolLoop ToolGate tool-loop details, or {@code null} when the agent ran without ToolGate
 */
public record LlmInvocationResult(
        ResolvedModelCall modelCall,
        String renderedPrompt,
        String response,
        AgentToolLoopResult toolLoop) {

    public LlmInvocationResult(ResolvedModelCall modelCall, String renderedPrompt, String response) {
        this(modelCall, renderedPrompt, response, null);
    }
}
