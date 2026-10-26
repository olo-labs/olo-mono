// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import io.ololabs.toolgate.contracts.*;
import io.ololabs.toolgate.service.*;

import javax.net.ssl.SSLSession;

/**
 * Exact body and peer session are captured at the HTTPS boundary; neither is reconstructed from
 * caller claims.
 */
public record ServiceRequest(
        ControlServiceAuthorization control,
        ServicePermit permit,
        ServicePermitVerifier.Request evidence,
        SSLSession peer) {
    public ServicePermitVerifier.Authorization authorize() {
        return control.authorize(permit, evidence, peer);
    }

    public Scope scope(ServicePermitVerifier.Authorization a) {
        return new Scope(evidence.tenant(), a.claims().sub(), evidence.operation());
    }

    public boolean read() {
        return evidence.operation().equals("memory.read")
                || evidence.operation().equals("rag.query");
    }
}
