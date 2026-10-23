/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Queries and calls the tools an agent identity is allowed to use, through an OLO ToolGate Gateway.
 *
 * <p>The agent identity is carried only by the workload bearer token resolved for {@code agentName}.
 * The catalog is looked up again on every call; callers must not cache it across runs.
 */
public interface ToolGateClient {

    /** Returns the tools currently enabled for {@code agentName}. */
    List<ToolGateTool> listTools(String agentName);

    /**
     * Runs one tool under {@code agentName}.
     *
     * @param requestId deterministic id (at most 128 characters); ToolGate maps a retry with the same id
     *                  and caller to the same invocation, so a finished result is returned, not re-run
     */
    ToolGateCallResult callTool(String agentName, String requestId, String toolName, JsonNode arguments);

    /**
     * The same Gateway, authenticated as the run's verified user with a fresh ToolGate user credential instead of an agent
     * workload token (M3.3). There is no fallback: without user credentials every call fails.
     *
     * @throws ToolGateException {@link ToolGateException.Kind#CREDENTIAL_MISSING} when user credentials are not configured
     */
    default ToolGateClient forUser(UserBinding binding) {
        throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING, "ToolGate user credentials are not configured");
    }

    /**
     * Path B (M3.10): the run's user as subject and the agent as actor. Each call sends the user's credential and the
     * agent's on-behalf-of workload token, so ToolGate requires both the user's grant and the agent's capability.
     *
     * @throws ToolGateException {@link ToolGateException.Kind#CREDENTIAL_MISSING} when user credentials are not configured
     */
    default ToolGateClient forUserViaAgent(UserBinding binding) {
        throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING, "ToolGate user credentials are not configured");
    }

    /**
     * Checks that the Gateway answers HTTP. No token is sent; any HTTP response, including 401, counts as reachable.
     */
    void probe();
}
