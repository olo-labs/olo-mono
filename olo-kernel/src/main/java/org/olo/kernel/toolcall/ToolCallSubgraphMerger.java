/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.SubgraphMergeResult;
import org.olo.kernel.toolcall.model.ToolCallValidationResult;

import java.util.List;

/**
 * Validates planner-produced tool/agent call JSON and merges execution subgraphs after the planner node.
 *
 * <p>Implementations live in {@link org.olo.kernel.toolcall.impl}; wire via {@link ToolCallFactories}.
 */
public interface ToolCallSubgraphMerger {

    /** Validates raw JSON against allow-lists for tools and optional agents. */
    ToolCallValidationResult validate(String rawJson, String allowedToolsJson);

    /** Validates with an explicit agent allow-list (orchestrator planners). */
    ToolCallValidationResult validate(String rawJson, String allowedToolsJson, String allowedAgentsJson);

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
