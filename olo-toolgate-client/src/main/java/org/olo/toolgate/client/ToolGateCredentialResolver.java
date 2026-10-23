/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

/**
 * Resolves the ToolGate workload bearer token for an OLO agent name.
 */
public interface ToolGateCredentialResolver {

    /**
     * @throws ToolGateException with {@link ToolGateException.Kind#CREDENTIAL_MISSING} when no token is configured
     */
    String resolveToken(String agentName);
}
