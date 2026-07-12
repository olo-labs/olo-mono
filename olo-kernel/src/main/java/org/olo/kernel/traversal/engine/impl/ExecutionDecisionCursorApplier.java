/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.engine.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.spi.node.NodeStatus;

import java.util.Map;

/**
 * Maps an {@link ExecutionDecision} from the strategy registry onto the next {@link TraversalCursor}.
 */
final class ExecutionDecisionCursorApplier {

    private ExecutionDecisionCursorApplier() {
    }

    static TraversalCursor apply(
            KernelRuntimeContext context,
            NodeDefinition node,
            int step,
            String currentNodeId,
            ExecutionDecision decision,
            String resultMessage) {
        Map<String, Object> variables = context.getVariableMap();

        if (decision.kind() == ExecutionDecision.Kind.FAILED) {
            String message = decision.failureMessage().orElse("dynamic graph expansion failed");
            TraversalDiagnostics.logTraversalFailed(currentNodeId, NodeStatus.FAILED, message);
            return TraversalCursor.failed(currentNodeId, step, NodeStatus.FAILED, message);
        }
        if (decision.kind() == ExecutionDecision.Kind.REEXECUTE) {
            String reexecuteNodeId = decision.nextNodeId().orElse(currentNodeId);
            TraversalDiagnostics.logStepExit(step, node.getId(), reexecuteNodeId, variables);
            // Step counter is not advanced so the same logical step re-runs after subgraph expansion.
            return TraversalCursor.running(reexecuteNodeId, step - 1, currentNodeId, resultMessage);
        }
        if (decision.kind() == ExecutionDecision.Kind.EXPAND_SUBGRAPH) {
            String expandedNodeId = decision.nextNodeId()
                    .orElseThrow(() -> new KernelException("dynamic subgraph expansion produced no entry node"));
            TraversalDiagnostics.logStepExit(step, node.getId(), expandedNodeId, variables);
            return TraversalCursor.running(expandedNodeId, step, currentNodeId, resultMessage);
        }
        if (decision.kind() == ExecutionDecision.Kind.END || decision.nextNodeId().isEmpty()) {
            TraversalDiagnostics.logStepExit(step, node.getId(), null, variables);
            return TraversalCursor.completed(currentNodeId, step, resultMessage);
        }

        String nextNodeId = decision.nextNodeId().get();
        TraversalDiagnostics.logStepExit(step, node.getId(), nextNodeId, variables);
        return TraversalCursor.running(nextNodeId, step, currentNodeId, resultMessage);
    }
}
