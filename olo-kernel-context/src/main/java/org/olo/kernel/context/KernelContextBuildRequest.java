package org.olo.kernel.context;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;

import java.util.Objects;

/**
 * Inputs required to build a {@link KernelRuntimeContext} for a queue execution.
 */
public final class KernelContextBuildRequest {

    private final String queue;
    private final String inputPayload;
    private final WorkflowInput workflowInput;
    private final WorkflowDefinition sourceGraph;

    private KernelContextBuildRequest(
            String queue,
            String inputPayload,
            WorkflowInput workflowInput,
            WorkflowDefinition sourceGraph) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.inputPayload = inputPayload;
        this.workflowInput = workflowInput;
        this.sourceGraph = Objects.requireNonNull(sourceGraph, "sourceGraph");
        if (inputPayload == null && workflowInput == null) {
            throw new IllegalArgumentException("inputPayload or workflowInput is required");
        }
        if (inputPayload != null && workflowInput != null) {
            throw new IllegalArgumentException("only one of inputPayload or workflowInput may be set");
        }
    }

    public static KernelContextBuildRequest of(String queue, String inputPayload, WorkflowDefinition sourceGraph) {
        return new KernelContextBuildRequest(queue, inputPayload, null, sourceGraph);
    }

    public static KernelContextBuildRequest of(String queue, WorkflowInput workflowInput, WorkflowDefinition sourceGraph) {
        return new KernelContextBuildRequest(queue, null, workflowInput, sourceGraph);
    }

    public String getQueue() {
        return queue;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public WorkflowInput getWorkflowInput() {
        return workflowInput;
    }

    public WorkflowDefinition getSourceGraph() {
        return sourceGraph;
    }
}
