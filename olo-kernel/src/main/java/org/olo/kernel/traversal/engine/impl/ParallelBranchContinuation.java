/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.traversal.engine.impl;

import org.olo.kernel.traversal.KernelExecutionSnapshot;

/** Advance branch continuations after an external resume, before computing the next activity's routing. */
public final class ParallelBranchContinuation {
    private ParallelBranchContinuation() {}
    public static KernelExecutionSnapshot afterResume(KernelExecutionSnapshot snapshot) {
        var context=snapshot.toContext();
        return TraversalSnapshotMapper.toSnapshot(context,snapshot.graphSession(),
                ParallelBranchExecutor.advance(context,TraversalCursor.fromSnapshot(snapshot)));
    }
}
