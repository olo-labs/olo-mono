// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.app.knowledge;

import org.junit.jupiter.api.Test;
import org.olo.toolgate.client.GovernedCalls;
import org.olo.toolgate.client.ToolGateException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GovernedReadRecoveryTest {
    @Test void transientReadFailureRecovers() {
        var attempts = new AtomicInteger();
        assertEquals("authorized", GovernedReadRecovery.read(() -> {
            if (attempts.incrementAndGet() < 3)
                throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "unavailable");
            return "authorized";
        }));
        assertEquals(3, attempts.get());
    }

    @Test void persistentTimeoutHasFiniteAttempts() {
        var attempts = new AtomicInteger();
        assertThrows(ToolGateException.class, () -> GovernedReadRecovery.read(() -> {
            attempts.incrementAndGet();
            throw new ToolGateException(ToolGateException.Kind.TIMEOUT, "timeout");
        }));
        assertEquals(3, attempts.get());
    }

    @Test void refusalsAndProtocolFailuresAreNeverRetried() {
        for (var kind : new ToolGateException.Kind[]{ToolGateException.Kind.UNAUTHORIZED,
                ToolGateException.Kind.FORBIDDEN, ToolGateException.Kind.CREDENTIAL_MISSING,
                ToolGateException.Kind.PROTOCOL}) {
            var attempts = new AtomicInteger();
            assertThrows(ToolGateException.class, () -> GovernedReadRecovery.read(() -> {
                attempts.incrementAndGet(); throw new ToolGateException(kind, "refused");
            }));
            assertEquals(1, attempts.get());
        }
    }

    @Test void unknownOutcomeIsNotRetried() {
        var attempts = new AtomicInteger();
        assertThrows(GovernedCalls.ReconciliationRequired.class, () -> GovernedReadRecovery.read(() -> {
            attempts.incrementAndGet(); throw new GovernedCalls.ReconciliationRequired("operation");
        }));
        assertEquals(1, attempts.get());
    }

    @Test void interruptionStopsRecovery() {
        Thread.currentThread().interrupt();
        try {
            var attempts = new AtomicInteger();
            assertThrows(ToolGateException.class, () -> GovernedReadRecovery.read(() -> {
                attempts.incrementAndGet();
                throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "unavailable");
            }));
            assertEquals(1, attempts.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
