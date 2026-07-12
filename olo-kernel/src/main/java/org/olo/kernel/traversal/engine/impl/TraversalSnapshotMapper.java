/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.engine.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * Converts an in-memory traversal cursor back into a serializable {@link KernelExecutionSnapshot}.
 */
final class TraversalSnapshotMapper {

    private TraversalSnapshotMapper() {
    }

    static KernelExecutionSnapshot toSnapshot(
            KernelRuntimeContext context, MutableGraphSession graphSession, TraversalCursor cursor) {
        KernelRuntimeContext refreshed = refreshedContext(context, graphSession);
        return KernelExecutionSnapshot.fromContext(
                refreshed,
                cursor.nextNodeId(),
                cursor.step(),
                cursor.status(),
                cursor.lastNodeId(),
                cursor.lastStatus(),
                cursor.message());
    }

    private static KernelRuntimeContext refreshedContext(
            KernelRuntimeContext context, MutableGraphSession graphSession) {
        return new KernelRuntimeContext(
                context.getQueue(),
                context.getInput(),
                graphSession.graph(),
                true,
                context.getVariables(),
                context.getOutputs());
    }
}
