/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.exception.KernelContextException;
import org.olo.kernel.context.graph.GraphIsolation;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.util.Objects;

/**
 * Builds a {@link KernelRuntimeContext} from queue input and the workflow graph for that queue.
 */
public final class KernelContextBuilder {

    private KernelContextBuilder() {
    }

    public static KernelRuntimeContext build(KernelContextBuildRequest request) {
        Objects.requireNonNull(request, "request");

        WorkflowInput input = request.getWorkflowInput() != null
                ? request.getWorkflowInput()
                : deserializeInput(request.getInputPayload());
        WorkflowDefinition isolatedGraph = WorkflowDefinition.copyOf(request.getSourceGraph());
        boolean graphReady = GraphIsolation.prepare(isolatedGraph);
        if (!graphReady) {
            throw new KernelContextException("graph isolation failed for queue " + request.getQueue());
        }

        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(isolatedGraph);
        return new KernelRuntimeContext(request.getQueue(), input, isolatedGraph, graphReady, variables);
    }

    private static WorkflowInput deserializeInput(String payload) {
        try {
            return WorkflowInput.fromJson(payload);
        } catch (RuntimeException e) {
            throw new KernelContextException("failed to deserialize workflow input", e);
        }
    }
}
