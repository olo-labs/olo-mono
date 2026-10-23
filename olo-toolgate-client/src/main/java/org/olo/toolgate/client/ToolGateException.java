/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

/**
 * A ToolGate request that could not be completed. Every kind fails closed: callers must not fall back to
 * running without governance.
 */
public final class ToolGateException extends RuntimeException {

    public enum Kind {
        /** No workload token is configured for the agent. */
        CREDENTIAL_MISSING,
        /** The Gateway rejected the token (HTTP 401). */
        UNAUTHORIZED,
        /** The Gateway or Control refused the request for this agent. */
        FORBIDDEN,
        /** The Gateway, Control or an executor is unreachable or overloaded. */
        UNAVAILABLE,
        /** The request did not finish within the configured timeout. */
        TIMEOUT,
        /** The request or response did not follow the MCP 2026-07-28 contract. */
        PROTOCOL
    }

    private final Kind kind;

    public ToolGateException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ToolGateException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
