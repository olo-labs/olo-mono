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
        AgentToolLoopResult toolLoop,
        String progress) {

    public LlmInvocationResult(ResolvedModelCall modelCall, String renderedPrompt, String response) {
        this(modelCall, renderedPrompt, response, null, null);
    }

    public LlmInvocationResult(ResolvedModelCall modelCall, String renderedPrompt, String response, AgentToolLoopResult toolLoop) {
        this(modelCall, renderedPrompt, response, toolLoop, null);
    }

    /** The turn recorded progress and continues at the next step (M5.1); there is no response yet. */
    public static LlmInvocationResult continuing(ResolvedModelCall modelCall, String renderedPrompt, String progress) {
        return new LlmInvocationResult(modelCall, renderedPrompt, null, null, progress);
    }

    public boolean isContinuing() {
        return progress != null;
    }
}
