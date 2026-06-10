package org.olo.kernel.context.graph;

import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Prepares an isolated workflow graph for execution.
 *
 * <p>Traversal-based validation and mutation will be added later. For now this returns {@code true}
 * without visiting nodes or edges.
 */
public final class GraphIsolation {

    private GraphIsolation() {
    }

    /**
     * Returns whether the isolated graph is ready for execution.
     */
    public static boolean prepare(WorkflowDefinition isolatedGraph) {
        return isolatedGraph != null;
    }
}
