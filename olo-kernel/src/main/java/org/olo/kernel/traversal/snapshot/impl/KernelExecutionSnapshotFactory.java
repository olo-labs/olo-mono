/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.snapshot.impl;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;

/**
 * Builds {@link KernelExecutionSnapshot} instances from live traversal context state.
 */
public final class KernelExecutionSnapshotFactory {

    private KernelExecutionSnapshotFactory() {
    }

    public static KernelExecutionSnapshot fromContext(KernelRuntimeContext context) {
        return fromContext(context, null, 0, KernelExecutionSnapshot.Status.RUNNING, null, null, null);
    }

    public static KernelExecutionSnapshot fromContext(
            KernelRuntimeContext context,
            String nextNodeId,
            int step,
            KernelExecutionSnapshot.Status status,
            String lastNodeId,
            org.olo.spi.node.NodeStatus lastStatus,
            String message) {
        WorkflowDefinition graph = context.getGraph();
        return new KernelExecutionSnapshot(
                context.getQueue(),
                context.getInput(),
                GraphSnapshotPolicy.maybeEmbedGraphJson(context.getQueue(), context.getInput(), graph),
                context.getVariableMap(),
                context.getOutputMap(),
                nextNodeId,
                step,
                status,
                lastNodeId,
                lastStatus,
                message,
                SnapshotSchedulingResolver.computeNextRequiresDedicatedActivity(graph, nextNodeId, status),
                NodeActivityNaming.formatWorkflow(graph),
                SnapshotSchedulingResolver.computeNextActivityName(graph, nextNodeId, status));
    }
}
