/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.engine.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.spi.node.NodeStatus;

/**
 * Mutable traversal position carried across single-step and full-graph execution loops.
 */
record TraversalCursor(
        String nextNodeId,
        int step,
        KernelExecutionSnapshot.Status status,
        String lastNodeId,
        NodeStatus lastStatus,
        String message) {

    static TraversalCursor running(String nextNodeId, int step, String lastNodeId, String message) {
        return new TraversalCursor(
                nextNodeId, step, KernelExecutionSnapshot.Status.RUNNING, lastNodeId, NodeStatus.COMPLETED, message);
    }

    static TraversalCursor completed(String lastNodeId, int step, String message) {
        return new TraversalCursor(
                null, step, KernelExecutionSnapshot.Status.COMPLETED, lastNodeId, NodeStatus.COMPLETED, message);
    }

    static TraversalCursor failed(String lastNodeId, int step, NodeStatus lastStatus, String message) {
        return new TraversalCursor(
                null, step, KernelExecutionSnapshot.Status.FAILED, lastNodeId, lastStatus, message);
    }

    static TraversalCursor fromSnapshot(KernelExecutionSnapshot snapshot) {
        return new TraversalCursor(
                snapshot.getNextNodeId(),
                snapshot.getStep(),
                snapshot.getStatus(),
                snapshot.getLastNodeId(),
                snapshot.getLastStatus(),
                snapshot.getMessage());
    }

    String resolveNextNodeId(GraphIndex index) {
        if (nextNodeId != null && !nextNodeId.isBlank()) {
            return nextNodeId;
        }
        return index.nodes().stream()
                .filter(node -> "START".equals(node.getType()))
                .map(NodeDefinition::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("workflow graph has no START node"));
    }
}
