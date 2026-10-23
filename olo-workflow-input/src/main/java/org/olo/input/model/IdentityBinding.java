/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The verified user a run acts for, as the backend saw it when it started the run (M3.3). Not a credential: the worker
 * exchanges it at the backend for a short-lived ToolGate user credential, and the backend rechecks the session, its
 * epoch and the tenant membership every time, so a revoked session or a removed member gets nothing.
 *
 * @param tenantId     the run's tenant, as verified by the backend
 * @param userId       the stable OLO user id, derived from the identity provider's issuer and subject
 * @param issuer       the identity provider issuer of the session
 * @param sessionId    the identity provider session ({@code sid})
 * @param sessionEpoch the session epoch when the run started; a revocation increases it
 */
public record IdentityBinding(String tenantId, String userId, String issuer, String sessionId, long sessionEpoch) {

    @JsonCreator
    public IdentityBinding(@JsonProperty("tenantId") String tenantId,
                           @JsonProperty("userId") String userId,
                           @JsonProperty("issuer") String issuer,
                           @JsonProperty("sessionId") String sessionId,
                           @JsonProperty("sessionEpoch") long sessionEpoch) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.issuer = issuer;
        this.sessionId = sessionId;
        this.sessionEpoch = sessionEpoch;
    }

    /** Whether every field is present; an incomplete binding is treated as no user. */
    public boolean complete() {
        return present(tenantId) && present(userId) && present(issuer) && present(sessionId) && sessionEpoch > 0;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
