/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.human;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.output.impl.ExecutionOutputApplier;
import org.olo.kernel.traversal.snapshot.impl.KernelExecutionSnapshotFactory;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Applies operator input after a {@code HUMAN} node pauses and advances traversal past the gate.
 */
public final class HumanInputResumeSupport {

    private static final ExecutionOutputApplier OUTPUT_APPLIER = new ExecutionOutputApplier();

    private HumanInputResumeSupport() {
    }

    public static KernelExecutionSnapshot resume(KernelExecutionSnapshot snapshot, HumanResumeInput input) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(input, "input");
        if (snapshot.getStatus() != KernelExecutionSnapshot.Status.WAITING) {
            throw new KernelException("cannot resume human input from snapshot status: " + snapshot.getStatus());
        }

        String waitingNodeId = snapshot.getLastNodeId();
        if (waitingNodeId == null || waitingNodeId.isBlank()) {
            waitingNodeId = snapshot.getNextNodeId();
        }
        if (waitingNodeId == null || waitingNodeId.isBlank()) {
            throw new KernelException("waiting snapshot has no human node id");
        }
        final String humanNodeId = waitingNodeId;

        KernelRuntimeContext context = snapshot.toContext();
        GraphIndex index = new DefaultGraphIndex(context.getGraph());
        NodeDefinition node = index.findNode(humanNodeId)
                .orElseThrow(() -> new KernelException("human node not found: " + humanNodeId));

        applyOperatorInput(context, input);
        NodeResult completed = completedHumanResult(node, input);
        OUTPUT_APPLIER.apply(context, node, completed);

        String nextNodeId = GraphEdgeNavigator.firstTarget(index, humanNodeId).orElse(null);
        KernelExecutionSnapshot.Status status = nextNodeId == null
                ? KernelExecutionSnapshot.Status.COMPLETED
                : KernelExecutionSnapshot.Status.RUNNING;
        String message = completed.message();

        return KernelExecutionSnapshotFactory.fromContext(
                context,
                nextNodeId,
                snapshot.getStep(),
                status,
                humanNodeId,
                NodeStatus.COMPLETED,
                message);
    }

    private static void applyOperatorInput(KernelRuntimeContext context, HumanResumeInput input) {
        String existingMessage = context.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE);
        String supplement = formatOperatorSupplement(input);
        String enrichedMessage = (existingMessage == null ? "" : existingMessage) + supplement;
        context.getVariables().set(MessageVariableInputBinder.MESSAGE_VARIABLE, enrichedMessage);
        context.getVariables().set("humanInputApproved", true);
        context.getVariables().set("approvalStatus", "approved");
        context.getVariables().set("approvedBy", input.resolvedApprover());
        if (!input.fields().isEmpty()) {
            context.getVariables().set("humanInputFields", input.fields());
        }
    }

    private static String formatOperatorSupplement(HumanResumeInput input) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\nOperator approval (human-input): approved by ")
                .append(input.resolvedApprover())
                .append('.');
        String comment = input.resolvedComment();
        if (!comment.isBlank()) {
            builder.append('\n').append(comment);
        }
        if (!input.fields().isEmpty()) {
            builder.append("\nStructured input: ").append(input.fields());
        }
        return builder.toString();
    }

    private static NodeResult completedHumanResult(NodeDefinition node, HumanResumeInput input) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("approvalStatus", "approved");
        output.put("approvedBy", input.resolvedApprover());
        if (!input.resolvedComment().isBlank()) {
            output.put("comment", input.resolvedComment());
        }
        if (!input.fields().isEmpty()) {
            output.put("fields", input.fields());
        }
        if (node.getApproval() != null && node.getApproval().getTitle() != null) {
            output.put("title", node.getApproval().getTitle());
        }
        String message = node.getApproval() != null && node.getApproval().getTitle() != null
                ? "Human input received: " + node.getApproval().getTitle()
                : "Human input received";
        return NodeResult.completed(message, output);
    }
}
