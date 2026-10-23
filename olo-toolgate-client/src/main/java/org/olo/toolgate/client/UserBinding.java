/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import java.util.Objects;

/**
 * Non-secret reference to the verified user a run acts for. The backend exchanges it for a ToolGate user credential
 * after rechecking the session, its epoch and the tenant membership.
 */
public record UserBinding(String tenantId, String userId, String issuer, String sessionId, long sessionEpoch) {

    public UserBinding {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(sessionId, "sessionId");
        if (sessionEpoch < 1) {
            throw new IllegalArgumentException("sessionEpoch must be positive");
        }
    }
}
