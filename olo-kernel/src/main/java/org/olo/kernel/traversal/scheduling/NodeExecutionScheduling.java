package org.olo.kernel.traversal.scheduling;

import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;

/**
 * Maps canvas execution metadata to Temporal scheduling boundaries.
 */
public final class NodeExecutionScheduling {

    private NodeExecutionScheduling() {
    }

    /**
     * Returns {@code true} when the node runs as its own Temporal activity ({@code id:label}).
     * <p>
     * Default is a dedicated activity per node. Only nodes with {@link ExecutionModel#INLINE}
     * and no {@link ExecutionKind#ACTIVITY} kind execute synchronously inside the workflow loop.
     * Dynamic graph planners keep {@code INLINE} for subgraph expansion but declare
     * {@code executionKind: ACTIVITY} so the LLM step still runs as an individual activity.
     */
    public static boolean requiresDedicatedActivity(NodeDefinition node) {
        ExecutionModel model = node.getExecutionModel();
        if (model == ExecutionModel.INLINE) {
            return node.getExecutionKind() == ExecutionKind.ACTIVITY;
        }
        return true;
    }

    /**
     * Returns {@code true} when the node executes in the workflow thread (not as a Temporal activity).
     */
    public static boolean runsInWorkflow(NodeDefinition node) {
        return !requiresDedicatedActivity(node);
    }
}
