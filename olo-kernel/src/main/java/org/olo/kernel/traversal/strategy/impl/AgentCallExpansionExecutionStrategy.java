/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.agentcall.AgentCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger;
import org.olo.kernel.dynamicgraph.DynamicSubgraphInjectionLogger.InjectionRecord;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.agentcall.AgentCallResultsSupport;
import org.olo.kernel.agentcall.AgentCallRetryVariables;
import org.olo.kernel.agentcall.AgentCallSubgraphMerger;
import org.olo.kernel.agentcall.model.ParsedAgentCall;
import org.olo.kernel.agentcall.model.AgentCallValidationResult;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * After an inline delegation planner completes, validates {@code agentCallSequenceJson},
 * inserts child-agent workflows, or continues directly when {@code directResponse} is set.
 */
public final class AgentCallExpansionExecutionStrategy implements ExecutionStrategy {

    private final AgentCallSubgraphMerger subgraphMerger;
    private final AgentCallRetryVariables retryVariables;

    public AgentCallExpansionExecutionStrategy() {
        this(org.olo.kernel.agentcall.AgentCallFactories.defaultAgentCallSubgraphMerger(),
                org.olo.kernel.agentcall.AgentCallFactories.defaultAgentCallRetryVariables());
    }

    public AgentCallExpansionExecutionStrategy(
            AgentCallSubgraphMerger subgraphMerger, AgentCallRetryVariables retryVariables) {
        this.subgraphMerger = Objects.requireNonNull(subgraphMerger, "subgraphMerger");
        this.retryVariables = Objects.requireNonNull(retryVariables, "retryVariables");
    }

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        return request.graphSession() != null
                && AgentCallPlannerSupport.isAgentCallPlanner(request.completedNode())
                && request.nodeResult().status() == NodeStatus.COMPLETED;
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        MutableGraphSession graphSession = request.graphSession();
        var node = request.completedNode();
        String outputVariable = AgentCallPlannerSupport.outputVariable(node);
        String availableAgentsJson = request.context()
                .getVariables()
                .getString(AgentCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE);
        String rawJson = request.context().getVariables().getString(outputVariable);
        AgentCallValidationResult validation =
                subgraphMerger.validate(rawJson, availableAgentsJson);
        if (!validation.valid()) {
            return handleInvalidJson(request, node, validation.message());
        }

        retryVariables.resetRetryCount(request.context().getVariables());
        request.context().getVariables().set(AgentCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, null);
        String continueNodeId = AgentCallPlannerSupport.continueNodeId(node, "end");

        if (validation.kind() == AgentCallValidationResult.Kind.DIRECT_RESPONSE) {
            request.context()
                    .getVariables()
                    .set(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME, validation.directResponse());
            return ExecutionDecision.linear(name(), continueNodeId);
        }

        List<ParsedAgentCall> pendingAgentCalls = AgentCallResultsSupport.filterPending(
                validation.agentCalls(), request.context().getVariables());

        if (pendingAgentCalls.isEmpty()) {
            return handleDuplicateDelegation(request, node, validation);
        }

        var mergeResult = subgraphMerger.merge(
                graphSession.graph(),
                node.getId(),
                continueNodeId,
                pendingAgentCalls);
        DynamicSubgraphInjectionLogger.logInjection(new InjectionRecord(
                InjectionRecord.Kind.AGENT_CALL,
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
        return "agent-call-expansion";
    }

    private ExecutionDecision handleInvalidJson(
            ExecutionStrategyRequest request, org.olo.definition.node.NodeDefinition node, String message) {
        int retries = retryVariables.readRetryCount(request.context().getVariables());
        int maxRetries = AgentCallPlannerSupport.maxInvalidJsonRetries(node);
        request.context()
                .getVariables()
                .set(AgentCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, message);
        if (retries + 1 >= maxRetries) {
            return ExecutionDecision.failed(
                    name(), "invalid agent call sequence JSON after " + maxRetries + " attempts: " + message);
        }
        retryVariables.incrementRetryCount(request.context().getVariables());
        return ExecutionDecision.reexecute(name(), node.getId());
    }

    private ExecutionDecision handleDuplicateDelegation(
            ExecutionStrategyRequest request,
            org.olo.definition.node.NodeDefinition node,
            AgentCallValidationResult validation) {
        int retries = retryVariables.readRetryCount(request.context().getVariables());
        int maxRetries = AgentCallPlannerSupport.maxInvalidJsonRetries(node);
        String duplicateMessage = duplicateDelegationMessage(validation);
        request.context()
                .getVariables()
                .set(AgentCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE, duplicateMessage);
        if (retries + 1 >= maxRetries) {
            return ExecutionDecision.failed(
                    name(),
                    "planner repeated already-dispatched agents after "
                            + maxRetries
                            + " attempts: "
                            + duplicateMessage);
        }
        retryVariables.incrementRetryCount(request.context().getVariables());
        request.context().getVariables().set(AgentCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE, null);
        return ExecutionDecision.reexecute(name(), node.getId());
    }

    private static String duplicateDelegationMessage(AgentCallValidationResult validation) {
        List<String> parts = new ArrayList<>();
        if (!validation.agentCalls().isEmpty()) {
            parts.add("agentCalls already completed for: "
                    + validation.agentCalls().stream()
                            .map(ParsedAgentCall::agentId)
                            .reduce((left, right) -> left + ", " + right)
                            .orElse(""));
        }
        return String.join("; ", parts)
                + ". Read agentResultsJson and return directResponse or delegate to a different agent.";
    }
}
