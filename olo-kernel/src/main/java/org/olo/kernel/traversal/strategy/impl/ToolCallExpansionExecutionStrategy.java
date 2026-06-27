package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger.InjectionRecord;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.toolcall.AvailableToolsJsonResolver;
import org.olo.kernel.toolcall.ToolCallSubgraphMerger;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeStatus;

/**
 * After an inline tool-call planner agent completes, validates {@code toolCallSequenceJson},
 * merges a tool execution subgraph, or continues directly when {@code directResponse} is set.
 */
public final class ToolCallExpansionExecutionStrategy implements ExecutionStrategy {

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        return request.graphSession() != null
                && ToolCallPlannerSupport.isToolCallPlanner(request.completedNode())
                && request.nodeResult().status() == NodeStatus.COMPLETED;
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        MutableGraphSession graphSession = request.graphSession();
        var node = request.completedNode();
        String outputVariable = ToolCallPlannerSupport.outputVariable(node);
        String availableToolsJson = request.context()
                .getVariables()
                .getString(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE);
        String rawJson = request.context().getVariables().getString(outputVariable);
        ToolCallSubgraphMerger.ValidationResult validation =
                ToolCallSubgraphMerger.validate(rawJson, availableToolsJson);
        if (!validation.valid()) {
            int retries = ToolCallSubgraphMerger.readRetryCount(request.context().getVariables());
            int maxRetries = ToolCallPlannerSupport.maxInvalidJsonRetries(node);
            request.context()
                    .getVariables()
                    .set(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, validation.message());
            if (retries + 1 >= maxRetries) {
                return ExecutionDecision.failed(
                        name(),
                        "invalid tool call sequence JSON after "
                                + maxRetries
                                + " attempts: "
                                + validation.message());
            }
            ToolCallSubgraphMerger.incrementRetryCount(request.context().getVariables());
            return ExecutionDecision.reexecute(name(), node.getId());
        }

        ToolCallSubgraphMerger.resetRetryCount(request.context().getVariables());
        request.context().getVariables().set(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, null);
        String continueNodeId = ToolCallPlannerSupport.continueNodeId(node, "end");

        if (validation.kind() == ToolCallSubgraphMerger.ValidationResult.Kind.DIRECT_RESPONSE) {
            request.context()
                    .getVariables()
                    .set(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME, validation.directResponse());
            return ExecutionDecision.linear(name(), continueNodeId);
        }

        ToolCallSubgraphMerger.resetToolResults(request.context().getVariables());
        ToolCallSubgraphMerger.MergeResult mergeResult = ToolCallSubgraphMerger.merge(
                graphSession.graph(),
                node.getId(),
                continueNodeId,
                validation.toolCalls());
        DynamicSubgraphInjectionLogger.logInjection(new InjectionRecord(
                InjectionRecord.Kind.TOOL_CALL,
                name(),
                request.context().getGraph().getId(),
                node.getId(),
                continueNodeId,
                mergeResult.entryNodeId(),
                validation.normalizedJson(),
                mergeResult.graph()));
        graphSession.replaceGraph(mergeResult.graph());
        return ExecutionDecision.expandSubgraph(name(), mergeResult.entryNodeId());
    }

    @Override
    public String name() {
        return "tool-call-expansion";
    }
}
