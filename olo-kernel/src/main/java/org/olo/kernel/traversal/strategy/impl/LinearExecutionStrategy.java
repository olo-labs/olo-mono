/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.strategy.impl;

import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;

/**
 * Default single-successor navigation for linear pipelines
 * (START → PLANNER → TOOL → AGENT → REVIEWER → END).
 */
public final class LinearExecutionStrategy implements ExecutionStrategy {

    public static final String STRATEGY_NAME = "linear";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        return true;
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        return GraphEdgeNavigator.firstTarget(request.graphIndex(), request.completedNode().getId())
                .map(next -> ExecutionDecision.linear(STRATEGY_NAME, next))
                .orElseGet(() -> ExecutionDecision.end(STRATEGY_NAME));
    }
}
