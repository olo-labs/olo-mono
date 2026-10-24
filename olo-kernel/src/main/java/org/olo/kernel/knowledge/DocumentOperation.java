// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import org.olo.toolgate.client.UserBinding;

import java.util.Map;

/**
 * Payloads contain identity references and exact arguments, never credentials; the tenant payload
 * codec is required.
 */
public record DocumentOperation(
        String tenantId,
        UserBinding user,
        String servicePrincipal,
        String operationId,
        String operation,
        String kbId,
        Map<String, Object> request) {
    public DocumentOperation {
        java.util.Objects.requireNonNull(tenantId);
        if (user != null && !tenantId.equals(user.tenantId()))
            throw new SecurityException("Document identity tenant mismatch");
        request = Map.copyOf(request);
    }
}
