/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.graph.visit.impl;

import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.graph.visit.NextNodeResolver;

import java.util.Optional;

/**
 * @deprecated Prefer {@link org.olo.kernel.traversal.strategy.impl.LinearExecutionStrategy} via
 * {@link org.olo.kernel.traversal.strategy.ExecutionStrategyRegistry}.
 */
@Deprecated
public final class SingleEdgeNextNodeResolver implements NextNodeResolver {

    @Override
    public Optional<String> resolveNext(GraphIndex index, String currentNodeId) {
        return GraphEdgeNavigator.firstTarget(index, currentNodeId);
    }
}
