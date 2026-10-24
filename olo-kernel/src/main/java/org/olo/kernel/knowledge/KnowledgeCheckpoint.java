// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * A separate recorded activity performs fresh checks before each model activity attempt and final
 * persistence.
 */
public final class KnowledgeCheckpoint {
    public static final String GUARDED = "__olo.knowledge.checked";

    private KnowledgeCheckpoint() {}

    public static KernelExecutionSnapshot check(
            KernelExecutionSnapshot snapshot, String checkpoint) {
        var context = snapshot.toContext();
        var lifecycle = KnowledgeRuntime.lifecycle();
        if (lifecycle != null) lifecycle.recheck(context, checkpoint);
        context.getVariables().set(GUARDED, true);
        context.getVariables().set(GUARDED + ".at", System.currentTimeMillis());
        context.getVariables().set(GUARDED + ".id", checkpoint);
        return KernelExecutionSnapshot.fromContext(
                context,
                snapshot.getNextNodeId(),
                snapshot.getStep(),
                snapshot.getStatus(),
                snapshot.getLastNodeId(),
                snapshot.getLastStatus(),
                snapshot.getMessage());
    }
}
