/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

/**
 * How an agent turn ended.
 */
public enum AgentToolOutcome {
    /** The model gave a final answer. */
    COMPLETED,
    /**
     * A tool call needs approval in ToolGate and nothing was run for it. Only {@link AgentToolLoop#run}, which has no
     * durable wait, stops here; a durable turn waits and resumes the original invocation once approved (M5.5).
     */
    WAITING_APPROVAL,
    /**
     * A call's outcome is unknown: it may have taken effect. It is never sent again; it is reconciled by its executor or
     * an operator (M5.4). Also used for a recorded call older than the dedup retention.
     */
    OUTCOME_UNKNOWN,
    /** An approval was not decided within the approval expiry. */
    APPROVAL_EXPIRED,
    /** The model kept calling tools until {@code maxToolRounds}; it was then asked to answer without tools. */
    ROUND_LIMIT
}
