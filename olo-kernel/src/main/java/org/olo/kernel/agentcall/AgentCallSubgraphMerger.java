/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agentcall;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.agentcall.model.ParsedAgentCall;
import org.olo.kernel.agentcall.model.ParsedToolCall;
import org.olo.kernel.agentcall.model.SubgraphMergeResult;
import org.olo.kernel.agentcall.model.AgentCallValidationResult;

import java.util.List;

/**
 * Validates planner-produced tool/agent call JSON and merges execution subgraphs after the planner node.
 *
 * <p>Implementations live in {@link org.olo.kernel.agentcall.impl}; wire via {@link AgentCallFactories}.
 */
public interface AgentCallSubgraphMerger {

    /** Validates raw JSON against allow-lists for tools and optional agents. */
    AgentCallValidationResult validate(String rawJson, String allowedToolsJson);

    /** Validates with an explicit agent allow-list (orchestrator planners). */
    AgentCallValidationResult validate(String rawJson, String allowedToolsJson, String allowedAgentsJson);

    /** Merges only tool nodes between planner and continue nodes. */
    SubgraphMergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedToolCall> toolCalls);

    /** Merges agent child-workflow chain, optionally followed by tool nodes. */
    SubgraphMergeResult mergeAgentAndToolCalls(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedAgentCall> agentCalls,
            List<ParsedToolCall> toolCalls);

    int readRetryCount(WorkflowRuntimeVariables variables);

    void incrementRetryCount(WorkflowRuntimeVariables variables);

    void resetRetryCount(WorkflowRuntimeVariables variables);

    void resetToolResults(WorkflowRuntimeVariables variables);
}
