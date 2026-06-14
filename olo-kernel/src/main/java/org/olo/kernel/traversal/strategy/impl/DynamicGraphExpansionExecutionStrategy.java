package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.kernel.dynamicgraph.DynamicSubgraphMerger;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeStatus;

/**
 * After an inline dynamic graph planner node completes, validates {@code generatedGraphJson},
 * merges a subgraph into the active graph, or re-executes the planner when JSON is invalid.
 */
public final class DynamicGraphExpansionExecutionStrategy implements ExecutionStrategy {

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        return request.graphSession() != null
                && DynamicGraphPlannerSupport.isDynamicGraphPlanner(request.completedNode())
                && request.nodeResult().status() == NodeStatus.COMPLETED;
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        MutableGraphSession graphSession = request.graphSession();
        var node = request.completedNode();
        String outputVariable = DynamicGraphPlannerSupport.outputVariable(node);
        String rawJson = request.context().getVariables().getString(outputVariable);
        DynamicSubgraphMerger.ValidationResult validation = DynamicSubgraphMerger.validate(rawJson);
        if (!validation.valid()) {
            int retries = DynamicSubgraphMerger.readRetryCount(request.context().getVariables());
            int maxRetries = DynamicGraphPlannerSupport.maxInvalidJsonRetries(node);
            request.context()
                    .getVariables()
                    .set(
                            DynamicGraphPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE,
                            validation.message());
            if (retries + 1 >= maxRetries) {
                return ExecutionDecision.failed(
                        name(),
                        "invalid generated graph JSON after "
                                + maxRetries
                                + " attempts: "
                                + validation.message());
            }
            DynamicSubgraphMerger.incrementRetryCount(request.context().getVariables());
            return ExecutionDecision.reexecute(name(), node.getId());
        }

        DynamicSubgraphMerger.resetRetryCount(request.context().getVariables());
        request.context().getVariables().set(DynamicGraphPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, null);
        String continueNodeId = DynamicGraphPlannerSupport.continueNodeId(node, "end");
        DynamicSubgraphMerger.MergeResult mergeResult = DynamicSubgraphMerger.merge(
                graphSession.graph(),
                node.getId(),
                continueNodeId,
                validation.normalizedJson());
        graphSession.replaceGraph(mergeResult.graph());
        return ExecutionDecision.expandSubgraph(name(), mergeResult.entryNodeId());
    }

    @Override
    public String name() {
        return "dynamic-graph-expansion";
    }
}
