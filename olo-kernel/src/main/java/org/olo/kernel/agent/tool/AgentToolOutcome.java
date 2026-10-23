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
    /** A tool call needs approval in ToolGate. Nothing was run for it; the run must be repeated after approval. */
    WAITING_APPROVAL,
    /** The model kept calling tools until {@code maxToolRounds}; it was then asked to answer without tools. */
    ROUND_LIMIT
}
