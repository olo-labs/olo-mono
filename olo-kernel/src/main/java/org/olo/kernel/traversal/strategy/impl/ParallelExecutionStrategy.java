package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.node.NodeType;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;

import java.util.List;

/**
 * Fan-out / join orchestration for {@link NodeType#PARALLEL} nodes.
 */
public final class ParallelExecutionStrategy implements ExecutionStrategy {

    public static final String STRATEGY_NAME = "parallel";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        return NodeType.PARALLEL.name().equals(request.completedNode().getType());
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        String parallelNodeId = request.completedNode().getId();
        List<String> branchEntryNodeIds = GraphEdgeNavigator.allTargets(request.graphIndex(), parallelNodeId);
        if (branchEntryNodeIds.isEmpty()) {
            return ExecutionDecision.end(STRATEGY_NAME);
        }
        String joinNodeId = GraphEdgeNavigator.findCommonJoinNode(request.graphIndex(), branchEntryNodeIds)
                .orElse(null);
        return ExecutionDecision.parallelFork(STRATEGY_NAME, branchEntryNodeIds, joinNodeId);
    }
}
