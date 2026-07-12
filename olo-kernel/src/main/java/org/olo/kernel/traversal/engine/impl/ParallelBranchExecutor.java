/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.engine.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.strategy.ExecutionDecision;

import java.util.List;

/**
 * Runs PARALLEL fork branches sequentially until each reaches the shared join node.
 */
final class ParallelBranchExecutor {

    @FunctionalInterface
    interface StepRunner {
        TraversalCursor executeSingleStep(
                KernelRuntimeContext context, MutableGraphSession graphSession, TraversalCursor cursor);
    }

    private ParallelBranchExecutor() {
    }

    static TraversalCursor execute(
            KernelRuntimeContext context,
            MutableGraphSession graphSession,
            ExecutionDecision decision,
            int step,
            String forkNodeId,
            String forkMessage,
            StepRunner stepRunner) {
        List<String> branchEntryNodeIds = decision.branchEntryNodeIds();
        if (branchEntryNodeIds.isEmpty()) {
            throw new KernelException("PARALLEL fork has no outgoing branches");
        }
        String joinNodeId = decision.joinNodeId()
                .orElseThrow(() -> new KernelException(
                        "PARALLEL fork could not resolve a common join node for branches: "
                                + branchEntryNodeIds));

        int branchStep = step;
        for (String branchStart : branchEntryNodeIds) {
            TraversalCursor branchCursor = TraversalCursor.running(branchStart, branchStep, forkNodeId, forkMessage);
            while (branchCursor.status() == KernelExecutionSnapshot.Status.RUNNING
                    && !joinNodeId.equals(branchCursor.resolveNextNodeId(graphSession.index()))) {
                branchCursor = stepRunner.executeSingleStep(context, graphSession, branchCursor);
            }
            if (branchCursor.status() != KernelExecutionSnapshot.Status.RUNNING) {
                return branchCursor;
            }
            branchStep = branchCursor.step();
        }

        return TraversalCursor.running(joinNodeId, branchStep, forkNodeId, forkMessage);
    }
}
