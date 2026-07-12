/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal;

import org.olo.spi.node.NodeStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of traversing a workflow graph against a {@link org.olo.kernel.context.KernelRuntimeContext}.
 */
public record TraversalResult(
        boolean completed,
        String lastNodeId,
        NodeStatus lastStatus,
        String message) {

    public TraversalResult {
        lastStatus = Objects.requireNonNull(lastStatus, "lastStatus");
    }

    public static TraversalResult completed(String lastNodeId, String message) {
        return new TraversalResult(true, lastNodeId, NodeStatus.COMPLETED, message);
    }

    public static TraversalResult failed(String lastNodeId, NodeStatus status, String message) {
        return new TraversalResult(false, lastNodeId, status, message);
    }

    public Optional<String> resolvedLastNodeId() {
        return Optional.ofNullable(lastNodeId).filter(value -> !value.isBlank());
    }
}
