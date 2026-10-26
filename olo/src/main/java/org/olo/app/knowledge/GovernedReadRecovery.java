// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.app.knowledge;

import org.olo.toolgate.client.ToolGateException;
import java.util.function.Supplier;

/** Bounded recovery for read-only operations. Never retry a policy or unknown-outcome result. */
final class GovernedReadRecovery {
    private GovernedReadRecovery() {}

    static <T> T read(Supplier<T> request) {
        for (int attempt = 0; ; attempt++) {
            try {
                return request.get();
            } catch (ToolGateException failure) {
                if (attempt >= 2 || (failure.kind() != ToolGateException.Kind.UNAVAILABLE
                        && failure.kind() != ToolGateException.Kind.TIMEOUT)) throw failure;
                try {
                    Thread.sleep((attempt + 1) * 100L
                            + java.util.concurrent.ThreadLocalRandom.current().nextLong(100));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE,
                            "Governed read recovery interrupted");
                }
            }
        }
    }
}
