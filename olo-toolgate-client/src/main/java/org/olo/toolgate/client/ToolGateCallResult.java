/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Outcome of one governed {@code tools/call}.
 *
 * <p>Tool-level failures (the executor ran and failed, the tool is not in the agent's catalog, or the call
 * waits for approval) come back as results with {@code error == true} so the model can react to them.
 * Infrastructure and authorization failures are thrown as {@link ToolGateException} instead.
 *
 * @param error             {@code true} when the tool did not produce a usable result
 * @param text              the text content blocks, joined with newlines
 * @param structuredContent {@code structuredContent}, or {@code null}
 * @param state             invocation state reported by ToolGate, for example {@code PENDING_APPROVAL}; may be {@code null}
 * @param invocationId      ToolGate invocation id when reported; may be {@code null}
 */
public record ToolGateCallResult(
        boolean error,
        String text,
        JsonNode structuredContent,
        String state,
        String invocationId) {

    public static final String STATE_PENDING_APPROVAL = "PENDING_APPROVAL";
    /** The invocation may or may not have taken effect; it is never sent again, only reconciled (M5.4). */
    public static final String STATE_OUTCOME_UNKNOWN = "OUTCOME_UNKNOWN";
    private static final java.util.Set<String> ENDED = java.util.Set.of("FAILED", "CANCELLED", "EXPIRED", "PARTIAL");

    public ToolGateCallResult {
        text = text == null ? "" : text;
    }

    public boolean pendingApproval() {
        return STATE_PENDING_APPROVAL.equals(state);
    }

    public boolean outcomeUnknown() {
        return STATE_OUTCOME_UNKNOWN.equals(state);
    }

    /** The invocation ended without a result: failed, denied or cancelled, expired, or only partly done. */
    public boolean ended() {
        return state != null && ENDED.contains(state);
    }

    public static ToolGateCallResult failure(String message) {
        return new ToolGateCallResult(true, message, null, null, null);
    }
}
