// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import io.temporal.failure.*;
import io.temporal.workflow.Workflow;

import org.olo.toolgate.client.GovernedCalls;

import java.time.Duration;

/**
 * Pending ToolGate approvals wait on a durable timer and resume the original activity arguments.
 */
public final class GovernedActivity {
    private GovernedActivity() {}

    public static <T> T execute(Duration timeout, String activity, Class<T> type, Object... args) {
        int refreshed = 0;
        while (true) {
            try {
                return Workflow.newUntypedActivityStub(
                                org.olo.kernel.temporal.KernelActivityOptions.of(timeout))
                        .execute(activity, type, args);
            } catch (ActivityFailure failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof ApplicationFailure app
                        && app.getType().equals(KnowledgeRuntime.CheckpointExpired.class.getName())
                        && args.length == 2
                        && args[1] instanceof String operation
                        && operation.startsWith("knowledge-check:")
                        && refreshed++ < 4) {
                    args = args.clone();
                    args[1] = "knowledge-check:" + Workflow.randomUUID();
                    continue;
                }
                if (!(cause instanceof ApplicationFailure app)
                        || !app.getType().equals(GovernedCalls.Pending.class.getName()))
                    throw failure;
                Workflow.sleep(Duration.ofSeconds(2));
            }
        }
    }

    /**
     * A fresh, durably recorded checkpoint precedes EVERY attempt; activity-level retries cannot
     * reuse old scope.
     */
    public static <T> T checked(
            Duration timeout,
            String activity,
            Class<T> type,
            org.olo.kernel.traversal.KernelExecutionSnapshot snapshot,
            Object... tail) {
        if (!snapshot.getVariables().containsKey(ConversationLifecycle.STATE)) {
            var args = new Object[tail.length + 1];
            args[0] = snapshot;
            System.arraycopy(tail, 0, args, 1, tail.length);
            return execute(timeout, activity, type, args);
        }
        for (int attempt = 1; ; attempt++) {
            var checked =
                    execute(
                            Duration.ofMinutes(1),
                            snapshot.getWorkflowActivityName(),
                            org.olo.kernel.traversal.KernelExecutionSnapshot.class,
                            snapshot,
                            "knowledge-check:" + Workflow.randomUUID());
            var args = new Object[tail.length + 1];
            args[0] = checked;
            System.arraycopy(tail, 0, args, 1, tail.length);
            var options =
                    org.olo.kernel.temporal.KernelActivityOptions.of(timeout).toBuilder()
                            .setRetryOptions(
                                    io.temporal.common.RetryOptions.newBuilder()
                                            .setMaximumAttempts(1)
                                            .build())
                            .build();
            try {
                while (true) {
                    try {
                        return Workflow.newUntypedActivityStub(options)
                                .execute(activity, type, args);
                    } catch (ActivityFailure pending) {
                        if (!(pending.getCause() instanceof ApplicationFailure app)
                                || !app.getType().equals(GovernedCalls.Pending.class.getName()))
                            throw pending;
                        Workflow.sleep(Duration.ofSeconds(2));
                    }
                }
            } catch (ActivityFailure failed) {
                if (failed.getCause() instanceof ApplicationFailure app
                        && (app.getType().equals(SecurityException.class.getName())
                                || app.getType()
                                        .equals(
                                                org.olo.toolgate.client.GovernedCalls
                                                        .ReconciliationRequired.class
                                                        .getName()))) throw failed;
                if (attempt >= 4) throw failed;
                Workflow.sleep(Duration.ofSeconds(attempt));
            }
        }
    }
}
