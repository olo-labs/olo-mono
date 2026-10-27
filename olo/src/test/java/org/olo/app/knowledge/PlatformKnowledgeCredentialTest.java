// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.app.knowledge;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.olo.toolgate.client.ToolGateException;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

class PlatformKnowledgeCredentialTest {
    @Test void temporaryCredentialFailuresRemainUnavailableRatherThanDenied() {
        for (int status : new int[] {429, 502, 503}) {
            var failure = assertThrows(ToolGateException.class,
                    () -> PlatformKnowledge.credential(ResponseEntity.status(status).body(Map.of())));
            assertEquals(ToolGateException.Kind.UNAVAILABLE, failure.kind());
        }
    }

    @Test void deniedAuthorityIsNeverRetriedAsTemporaryFailure() {
        for (int status : new int[] {401, 403})
            assertThrows(SecurityException.class,
                    () -> PlatformKnowledge.credential(ResponseEntity.status(status).body(Map.of())));
    }

    @Test void aSuccessfulResponseMustContainANonemptyCredential() {
        assertEquals("fixture-token", PlatformKnowledge.credential(ResponseEntity.ok(Map.of("accessToken", "fixture-token"))));
        var failure = assertThrows(ToolGateException.class,
                () -> PlatformKnowledge.credential(ResponseEntity.ok(Map.of())));
        assertEquals(ToolGateException.Kind.PROTOCOL, failure.kind());
    }
}
