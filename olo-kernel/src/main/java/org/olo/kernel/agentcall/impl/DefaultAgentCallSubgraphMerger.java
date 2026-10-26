/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agentcall.impl;

import org.olo.definition.agentcall.AgentCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.agentcall.AgentCallSubgraphMerger;
import org.olo.kernel.agentcall.model.ParsedAgentCall;
import org.olo.kernel.agentcall.model.ParsedToolCall;
import org.olo.kernel.agentcall.model.SubgraphMergeResult;
import org.olo.kernel.agentcall.model.AgentCallValidationResult;

import java.util.List;

/**
 * Default {@link AgentCallSubgraphMerger} wiring JSON validation and subgraph splice helpers.
 */
public final class DefaultAgentCallSubgraphMerger implements AgentCallSubgraphMerger {

    @Override
    public AgentCallValidationResult validate(String rawJson, String allowedToolsJson) {
        return validate(rawJson, allowedToolsJson, null);
    }

    @Override
    public AgentCallValidationResult validate(String rawJson, String allowedToolsJson, String allowedAgentsJson) {
        return AgentCallSequenceValidator.validate(rawJson, allowedToolsJson, allowedAgentsJson);
    }

    @Override
    public SubgraphMergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedToolCall> toolCalls) {
        return mergeAgentAndToolCalls(graph, plannerNodeId, continueNodeId, List.of(), toolCalls);
    }

    @Override
    public SubgraphMergeResult mergeAgentAndToolCalls(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedAgentCall> agentCalls,
            List<ParsedToolCall> toolCalls) {
        return AgentCallChainMerger.merge(graph, plannerNodeId, continueNodeId, agentCalls, toolCalls);
    }

    @Override
    public int readRetryCount(WorkflowRuntimeVariables variables) {
        return readInt(variables, AgentCallPlannerSupport.DEFAULT_RETRY_VARIABLE);
    }

    @Override
    public void incrementRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(AgentCallPlannerSupport.DEFAULT_RETRY_VARIABLE, readRetryCount(variables) + 1);
    }

    @Override
    public void resetRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(AgentCallPlannerSupport.DEFAULT_RETRY_VARIABLE, 0);
    }

    @Override
    public void resetToolResults(WorkflowRuntimeVariables variables) {
        variables.set(AgentCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE, "[]");
    }

    private static int readInt(WorkflowRuntimeVariables variables, String key) {
        Object raw = variables.get(key);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
