/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What happened to one tool call, recorded in the node output. Arguments and results are deliberately left
 * out: node output lands in Temporal history, and ToolGate keeps the audited record of the call.
 *
 * @param tool         ToolGate tool name
 * @param requestId    the JSON-RPC id sent to ToolGate
 * @param round        tool round within the turn (1-based)
 * @param status       {@code OK}, {@code ERROR}, {@code PENDING_APPROVAL} or {@code INVALID_CALL}
 * @param invocationId ToolGate invocation id when reported
 */
public record ToolInvocationRecord(String tool, String requestId, int round, String status, String invocationId) {

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String INVALID_CALL = "INVALID_CALL";
    public static final String OUTCOME_UNKNOWN = "OUTCOME_UNKNOWN";

    public Map<String, Object> toOutput() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tool", tool);
        out.put("requestId", requestId);
        out.put("round", round);
        out.put("status", status);
        if (invocationId != null) {
            out.put("invocationId", invocationId);
        }
        return out;
    }
}
