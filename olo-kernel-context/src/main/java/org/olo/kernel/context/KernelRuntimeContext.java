/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.ExecutionOutputs;
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
    private final ExecutionOutputs outputs;

    public KernelRuntimeContext(
            String queue,
            WorkflowInput input,
            WorkflowDefinition graph,
            boolean graphReady,
            WorkflowRuntimeVariables variables) {
        this(queue, input, graph, graphReady, variables, new ExecutionOutputs());
    }

    public KernelRuntimeContext(
            String queue,
            WorkflowInput input,
            WorkflowDefinition graph,
            boolean graphReady,
            WorkflowRuntimeVariables variables,
            ExecutionOutputs outputs) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.input = Objects.requireNonNull(input, "input");
        this.graph = Objects.requireNonNull(graph, "graph");
        this.graphReady = graphReady;
        this.variables = Objects.requireNonNull(variables, "variables");
        this.outputs = Objects.requireNonNull(outputs, "outputs");
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
     * Whether the graph isolation step succeeded. Graph traversal and output generation are
     * performed by {@code olo-kernel} after the context is built.
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

    /**
     * Named outputs produced by graph nodes during this execution (planner, researcher, final, …).
     */
    public ExecutionOutputs getOutputs() {
        return outputs;
    }

    /**
     * Snapshot of execution output keys to captured values.
     */
    public Map<String, ExecutionOutput> getOutputMap() {
        return outputs.toMap();
    }
}
