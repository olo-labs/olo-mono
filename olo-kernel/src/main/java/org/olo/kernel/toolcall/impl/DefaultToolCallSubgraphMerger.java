/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.ToolCallSubgraphMerger;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.SubgraphMergeResult;
import org.olo.kernel.toolcall.model.ToolCallValidationResult;

import java.util.List;

/**
 * Default {@link ToolCallSubgraphMerger} wiring JSON validation and subgraph splice helpers.
 */
public final class DefaultToolCallSubgraphMerger implements ToolCallSubgraphMerger {

    @Override
    public ToolCallValidationResult validate(String rawJson, String allowedToolsJson) {
        return validate(rawJson, allowedToolsJson, null);
    }

    @Override
    public ToolCallValidationResult validate(String rawJson, String allowedToolsJson, String allowedAgentsJson) {
        return ToolCallSequenceValidator.validate(rawJson, allowedToolsJson, allowedAgentsJson);
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
        return readInt(variables, ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE);
    }

    @Override
    public void incrementRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE, readRetryCount(variables) + 1);
    }

    @Override
    public void resetRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE, 0);
    }

    @Override
    public void resetToolResults(WorkflowRuntimeVariables variables) {
        variables.set(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE, "[]");
    }

    private static int readInt(WorkflowRuntimeVariables variables, String key) {
        Object raw = variables.get(key);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
