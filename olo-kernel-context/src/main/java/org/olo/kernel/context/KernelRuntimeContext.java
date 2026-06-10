package org.olo.kernel.context;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable runtime context for a single queue execution: deserialized invocation input and an
 * isolated copy of the workflow graph.
 */
public final class KernelRuntimeContext {

    private final String queue;
    private final WorkflowInput input;
    private final WorkflowDefinition graph;
    private final boolean graphReady;
    private final WorkflowRuntimeVariables variables;

    public KernelRuntimeContext(
            String queue,
            WorkflowInput input,
            WorkflowDefinition graph,
            boolean graphReady,
            WorkflowRuntimeVariables variables) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.input = Objects.requireNonNull(input, "input");
        this.graph = Objects.requireNonNull(graph, "graph");
        this.graphReady = graphReady;
        this.variables = Objects.requireNonNull(variables, "variables");
    }

    public String getQueue() {
        return queue;
    }

    public WorkflowInput getInput() {
        return input;
    }

    public WorkflowDefinition getGraph() {
        return graph;
    }

    /**
     * Whether the graph isolation step succeeded. Currently a stub that returns {@code true}
     * without traversing nodes or edges.
     */
    public boolean isGraphReady() {
        return graphReady;
    }

    public WorkflowRuntimeVariables getVariables() {
        return variables;
    }

    /**
     * All workflow variables declared on the graph and their values for this execution.
     */
    public Map<String, Object> getVariableMap() {
        return variables.toMap();
    }
}
