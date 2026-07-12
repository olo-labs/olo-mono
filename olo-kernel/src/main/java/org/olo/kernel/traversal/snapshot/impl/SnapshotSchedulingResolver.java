/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.snapshot.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;
import org.olo.kernel.traversal.scheduling.NodeExecutionScheduling;

/**
 * Resolves Temporal activity routing metadata for the next snapshot node.
 */
public final class SnapshotSchedulingResolver {

    private SnapshotSchedulingResolver() {
    }

    public static boolean computeNextRequiresDedicatedActivity(
            WorkflowDefinition graph, String nextNodeId, KernelExecutionSnapshot.Status status) {
        if (status != KernelExecutionSnapshot.Status.RUNNING) {
            return false;
        }
        String resolvedNextNodeId = resolveStartOrExplicitNodeId(graph, nextNodeId);
        if (resolvedNextNodeId == null) {
            return true;
        }
        final String lookupNodeId = resolvedNextNodeId;
        GraphIndex index = new DefaultGraphIndex(graph);
        return index.findNode(lookupNodeId)
                .map(NodeExecutionScheduling::requiresDedicatedActivity)
                .orElse(true);
    }

    public static String computeNextActivityName(
            WorkflowDefinition graph, String nextNodeId, KernelExecutionSnapshot.Status status) {
        if (status != KernelExecutionSnapshot.Status.RUNNING) {
            return null;
        }
        String resolvedNextNodeId = resolveStartOrExplicitNodeId(graph, nextNodeId);
        if (resolvedNextNodeId == null) {
            return null;
        }
        final String lookupNodeId = resolvedNextNodeId;
        GraphIndex index = new DefaultGraphIndex(graph);
        return index.findNode(lookupNodeId).map(NodeActivityNaming::formatNode).orElse(null);
    }

    private static String resolveStartOrExplicitNodeId(WorkflowDefinition graph, String nextNodeId) {
        if (nextNodeId != null && !nextNodeId.isBlank()) {
            return nextNodeId;
        }
        GraphIndex index = new DefaultGraphIndex(graph);
        return index.nodes().stream()
                .filter(node -> "START".equals(node.getType()))
                .map(NodeDefinition::getId)
                .findFirst()
                .orElse(null);
    }
}
