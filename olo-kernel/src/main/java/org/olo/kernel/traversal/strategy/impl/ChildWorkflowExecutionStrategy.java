package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeType;
import org.olo.kernel.childworkflow.ChildWorkflowCoordinator;
import org.olo.kernel.childworkflow.DefaultChildWorkflowCoordinator;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;

/**
 * Selects child-workflow navigation path for {@link NodeType#AGENT} and {@link NodeType#WORKFLOW_REF}.
 */
public final class ChildWorkflowExecutionStrategy implements ExecutionStrategy {

    public static final String STRATEGY_NAME = "child-workflow";

    private final ChildWorkflowCoordinator coordinator;

    public ChildWorkflowExecutionStrategy() {
        this(new DefaultChildWorkflowCoordinator());
    }

    ChildWorkflowExecutionStrategy(ChildWorkflowCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        if (request.completedNode().getExecutionModel() != ExecutionModel.CHILD_WORKFLOW) {
            return false;
        }
        String type = request.completedNode().getType();
        return NodeType.AGENT.name().equals(type) || NodeType.WORKFLOW_REF.name().equals(type);
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        return coordinator
                .nextNodeId(
                        request.context(),
                        request.graphIndex(),
                        request.completedNode(),
                        request.nodeResult())
                .map(next -> ExecutionDecision.linear(STRATEGY_NAME, next))
                .orElseGet(() -> GraphEdgeNavigator.firstTarget(
                                request.graphIndex(), request.completedNode().getId())
                        .map(next -> ExecutionDecision.linear(STRATEGY_NAME, next))
                        .orElseGet(() -> ExecutionDecision.end(STRATEGY_NAME)));
    }
}
