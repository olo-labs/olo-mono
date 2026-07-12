/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.output.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.WorkflowReturnOutput;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.output.NodeOutputApplier;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.Map;

/**
 * Records each completed node into {@link org.olo.kernel.context.output.ExecutionOutputs} and optionally
 * mirrors the designated return slot into the legacy return variable.
 */
public final class ExecutionOutputApplier implements NodeOutputApplier {

    @Override
    public void apply(KernelRuntimeContext context, NodeDefinition node, NodeResult result) {
        if (result == null) {
            TraversalDiagnostics.logOutputApply(node.getId(), null, "skip", null, "node result is null");
            return;
        }
        if (result.status() != NodeStatus.COMPLETED) {
            TraversalDiagnostics.logOutputApply(
                    node.getId(),
                    null,
                    "skip",
                    null,
                    "node status is " + result.status());
            return;
        }

        String inboundMessage = context.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE);
        if (inboundMessage == null || inboundMessage.isBlank()) {
            TraversalDiagnostics.logOutputApply(
                    node.getId(),
                    null,
                    "skip",
                    null,
                    "inbound message variable is blank");
            return;
        }

        Object response = result.output().get("response");
        String message = result.message();
        if (response == null && (message == null || message.isBlank())) {
            TraversalDiagnostics.logOutputApply(
                    node.getId(),
                    null,
                    "skip",
                    null,
                    "node result has no output.response or result.message");
            return;
        }

        String outputKey = WorkflowReturnOutput.outputKeyForNode(node);
        ExecutionOutput captured = new ExecutionOutput(
                node.getId(),
                node.getType(),
                response,
                message,
                result.output());
        context.getOutputs().put(outputKey, captured);
        TraversalDiagnostics.logExecutionOutput(node.getId(), outputKey, captured);

        String returnVariable = WorkflowReturnVariable.resolveName(context.getGraph());
        if (returnVariable == null) {
            return;
        }
        if (!WorkflowReturnOutput.shouldMirrorToReturnVariable(context.getGraph(), node, returnVariable)) {
            TraversalDiagnostics.logOutputApply(
                    node.getId(),
                    returnVariable,
                    "skip-mirror",
                    captured.asReturnMessage(),
                    "returnOutputKey does not match node output slot " + outputKey);
            return;
        }

        String returnMessage = captured.asReturnMessage();
        if (returnMessage != null) {
            context.getVariables().set(returnVariable, returnMessage);
            TraversalDiagnostics.logOutputApply(
                    node.getId(), returnVariable, "mirror-from-execution-output", returnMessage, null);
        }
    }
}
