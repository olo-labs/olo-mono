// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Worker-installed platform capability; never derived from an agent's settings or selected tool
 * catalog.
 */
public final class KnowledgeRuntime {
    private static volatile ConversationLifecycle lifecycle;
    private static volatile org.olo.toolgate.client.GovernedCalls documents;

    private KnowledgeRuntime() {}

    public static void install(ConversationLifecycle value) {
        lifecycle = java.util.Objects.requireNonNull(value);
    }

    public static void installDocuments(org.olo.toolgate.client.GovernedCalls value) {
        documents = java.util.Objects.requireNonNull(value);
    }

    public static String document(DocumentOperation op) {
        String workflowTenant =
                org.olo.input.codec.TenantWorkflowIds.tenantOf(
                        io.temporal.activity.Activity.getExecutionContext()
                                .getInfo()
                                .getWorkflowId());
        if (!op.tenantId().equals(workflowTenant))
            throw new SecurityException(
                    "Document operation differs from its encrypted workflow tenant");
        var calls = documents;
        if (calls == null)
            throw new SecurityException("Governed document calls are not configured");
        if (op.user() != null) {
            if (op.servicePrincipal() != null) throw new SecurityException("Ambiguous principal");
            return calls.user(op.user(), op.operationId(), op.operation(), op.kbId(), op.request())
                    .toString();
        }
        if (!"olo-platform-service".equals(op.servicePrincipal()))
            throw new SecurityException("Reserved indexing principal required");
        // Every scheduled run has a new logical operation; retries within that run retain the same
        // one.
        return calls.service(
                        op.tenantId(),
                        op.servicePrincipal(),
                        op.operationId()
                                + ":"
                                + io.temporal.activity.Activity.getExecutionContext()
                                        .getInfo()
                                        .getRunId(),
                        op.operation(),
                        op.kbId(),
                        op.request())
                .toString();
    }

    public static void reset() {
        lifecycle = null;
        documents = null;
    }

    public static ConversationLifecycle lifecycle() {
        return lifecycle;
    }

    public static void beforeModel(KernelRuntimeContext context, String checkpoint) {
        var value = lifecycle;
        if (value == null) return;
        if (!Boolean.TRUE.equals(context.getVariables().get(KnowledgeCheckpoint.GUARDED)))
            throw new SecurityException(
                    "A recorded authorization checkpoint is required before model execution");
        Object at = context.getVariables().get(KnowledgeCheckpoint.GUARDED + ".at");
        long now = System.currentTimeMillis();
        if (!(at instanceof Number number)
                || number.longValue() > now
                || now - number.longValue() > 10000) throw new CheckpointExpired();
    }

    public static final class CheckpointExpired extends RuntimeException {
        public CheckpointExpired() {
            super("Authorization checkpoint expired while queued; refresh required");
        }
    }

    public static String prompt(KernelRuntimeContext context, String rendered) {
        var value = lifecycle;
        return value == null ? rendered : value.prompt(context, rendered);
    }
}
