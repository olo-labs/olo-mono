package org.olo.kernel.dynamicgraph;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;

/**
 * Mutable workflow graph used during traversal for inline dynamic subgraph expansion.
 */
public final class MutableGraphSession {

    private WorkflowDefinition graph;
    private GraphIndex index;

    public MutableGraphSession(WorkflowDefinition graph) {
        this.graph = graph;
        rebuildIndex();
    }

    public WorkflowDefinition graph() {
        return graph;
    }

    public GraphIndex index() {
        return index;
    }

    public void replaceGraph(WorkflowDefinition updatedGraph) {
        this.graph = updatedGraph;
        rebuildIndex();
    }

    private void rebuildIndex() {
        this.index = new DefaultGraphIndex(graph);
    }
}
