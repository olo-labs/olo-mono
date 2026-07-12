/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger.InjectionRecord;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.toolcall.AgentCallResultsSupport;
import org.olo.kernel.toolcall.ToolCallRetryVariables;
import org.olo.kernel.toolcall.ToolCallSubgraphMerger;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ToolCallValidationResult;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * After an inline tool-call planner agent completes, validates {@code toolCallSequenceJson},
 * merges a tool/agent execution subgraph, or continues directly when {@code directResponse} is set.
 */
public final class ToolCallExpansionExecutionStrategy implements ExecutionStrategy {

    private final ToolCallSubgraphMerger subgraphMerger;
    private final ToolCallRetryVariables retryVariables;

    public ToolCallExpansionExecutionStrategy() {
        this(org.olo.kernel.toolcall.ToolCallFactories.defaultToolCallSubgraphMerger(),
                org.olo.kernel.toolcall.ToolCallFactories.defaultToolCallRetryVariables());
    }

    public ToolCallExpansionExecutionStrategy(
            ToolCallSubgraphMerger subgraphMerger, ToolCallRetryVariables retryVariables) {
        this.subgraphMerger = Objects.requireNonNull(subgraphMerger, "subgraphMerger");
        this.retryVariables = Objects.requireNonNull(retryVariables, "retryVariables");
    }

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
        String availableAgentsJson = request.context()
                .getVariables()
                .getString(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE);
        String rawJson = request.context().getVariables().getString(outputVariable);
        ToolCallValidationResult validation =
                subgraphMerger.validate(rawJson, availableToolsJson, availableAgentsJson);
        if (!validation.valid()) {
            return handleInvalidJson(request, node, validation.message());
        }

        retryVariables.resetRetryCount(request.context().getVariables());
        request.context().getVariables().set(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, null);
        String continueNodeId = ToolCallPlannerSupport.continueNodeId(node, "end");

        if (validation.kind() == ToolCallValidationResult.Kind.DIRECT_RESPONSE) {
            request.context()
                    .getVariables()
                    .set(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME, validation.directResponse());
            return ExecutionDecision.linear(name(), continueNodeId);
        }

        List<ParsedAgentCall> pendingAgentCalls = AgentCallResultsSupport.filterPending(
                validation.agentCalls(), request.context().getVariables());
        var pendingToolCalls = validation.toolCalls();

        if (pendingAgentCalls.isEmpty() && pendingToolCalls.isEmpty()) {
            return handleDuplicateDelegation(request, node, validation);
        }

        retryVariables.resetToolResults(request.context().getVariables());
        var mergeResult = subgraphMerger.mergeAgentAndToolCalls(
                graphSession.graph(),
                node.getId(),
                continueNodeId,
                pendingAgentCalls,
                pendingToolCalls);
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

    private ExecutionDecision handleInvalidJson(
            ExecutionStrategyRequest request, org.olo.definition.node.NodeDefinition node, String message) {
        int retries = retryVariables.readRetryCount(request.context().getVariables());
        int maxRetries = ToolCallPlannerSupport.maxInvalidJsonRetries(node);
        request.context()
                .getVariables()
                .set(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, message);
        if (retries + 1 >= maxRetries) {
            return ExecutionDecision.failed(
                    name(), "invalid tool call sequence JSON after " + maxRetries + " attempts: " + message);
        }
        retryVariables.incrementRetryCount(request.context().getVariables());
        return ExecutionDecision.reexecute(name(), node.getId());
    }

    private ExecutionDecision handleDuplicateDelegation(
            ExecutionStrategyRequest request,
            org.olo.definition.node.NodeDefinition node,
            ToolCallValidationResult validation) {
        int retries = retryVariables.readRetryCount(request.context().getVariables());
        int maxRetries = ToolCallPlannerSupport.maxInvalidJsonRetries(node);
        String duplicateMessage = duplicateDelegationMessage(validation);
        request.context()
                .getVariables()
                .set(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, duplicateMessage);
        if (retries + 1 >= maxRetries) {
            return ExecutionDecision.failed(
                    name(),
                    "planner repeated already-dispatched agents or tools after "
                            + maxRetries
                            + " attempts: "
                            + duplicateMessage);
        }
        retryVariables.incrementRetryCount(request.context().getVariables());
        request.context().getVariables().set(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE, null);
        return ExecutionDecision.reexecute(name(), node.getId());
    }

    private static String duplicateDelegationMessage(ToolCallValidationResult validation) {
        List<String> parts = new ArrayList<>();
        if (!validation.agentCalls().isEmpty()) {
            parts.add("agentCalls already completed for: "
                    + validation.agentCalls().stream()
                            .map(ParsedAgentCall::agentId)
                            .reduce((left, right) -> left + ", " + right)
                            .orElse(""));
        }
        if (!validation.toolCalls().isEmpty()) {
            parts.add("toolCalls were already executed or tools are unavailable");
        }
        return String.join("; ", parts)
                + ". Read agentResultsJson and return directResponse or delegate to a different agent.";
    }
}
