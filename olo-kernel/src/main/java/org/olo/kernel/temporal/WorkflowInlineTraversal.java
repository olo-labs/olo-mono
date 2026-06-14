package org.olo.kernel.temporal;

import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * Executes a single INLINE-scheduled graph step inside the Temporal workflow loop.
 * Non-inline nodes are scheduled as individual {@code id:label} activities instead.
 */
public final class WorkflowInlineTraversal {

    private WorkflowInlineTraversal() {
    }

    public static KernelExecutionSnapshot executeStep(KernelExecutionSnapshot snapshot) {
        return KernelEntryPoint.executeTraversalStep(snapshot);
    }
}
