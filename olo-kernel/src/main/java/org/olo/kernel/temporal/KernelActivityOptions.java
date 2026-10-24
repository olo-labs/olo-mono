/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Bounded retries for kernel activities (M5.6). Temporal's default policy retries forever; here a failed step is retried
 * a few times with backoff, then fails the run. Retrying is safe because an AGENT turn records each decision before it
 * dispatches anything, and a dispatch re-sends the same logical-operation ids, which ToolGate deduplicates (M5.1).
 */
public final class KernelActivityOptions {

    /** Attempts per activity, including the first. */
    public static final int MAXIMUM_ATTEMPTS = 4;

    private KernelActivityOptions() {
    }

    public static ActivityOptions of(Duration startToClose) {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToClose)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setDoNotRetry(SecurityException.class.getName(),
                                org.olo.toolgate.client.GovernedCalls.Pending.class.getName(),
                                org.olo.toolgate.client.GovernedCalls.ReconciliationRequired.class.getName())
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setMaximumAttempts(MAXIMUM_ATTEMPTS)
                        .build())
                .build();
    }
}
