/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.context.impl;

import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.spi.context.ExecutionContext;

public final class VariableScopeBridge {

    private VariableScopeBridge() {
    }

    public static DefaultExecutionContext toExecutionContext(KernelRuntimeContext context, String nodeId) {
        DefaultExecutionContext executionContext = new DefaultExecutionContext(
                context.getGraph().getId(),
                resolveRunId(context),
                context.getQueue(),
                resolveCorrelationId(context));
        executionContext.setNodeId(nodeId);
        copyToExecutionContext(context.getVariables(), executionContext);
        return executionContext;
    }

    public static void copyFromExecutionContext(
            ExecutionContext executionContext, WorkflowRuntimeVariables variables) {
        for (var entry : executionContext.getVariables().entrySet()) {
            variables.set(entry.getKey(), entry.getValue());
        }
    }

    private static void copyToExecutionContext(
            WorkflowRuntimeVariables variables, DefaultExecutionContext executionContext) {
        for (var entry : variables.toMap().entrySet()) {
            executionContext.setVariable(entry.getKey(), entry.getValue());
        }
    }

    private static String resolveRunId(KernelRuntimeContext context) {
        if (context.getInput().getContext() != null
                && context.getInput().getContext().getRunId() != null
                && !context.getInput().getContext().getRunId().isBlank()) {
            return context.getInput().getContext().getRunId();
        }
        if (context.getInput().getRouting() != null
                && context.getInput().getRouting().getTransactionId() != null
                && !context.getInput().getRouting().getTransactionId().isBlank()) {
            return context.getInput().getRouting().getTransactionId();
        }
        return "run";
    }

    private static String resolveCorrelationId(KernelRuntimeContext context) {
        if (context.getInput().getContext() == null) {
            return null;
        }
        return context.getInput().getContext().getCorrelationId();
    }
}
