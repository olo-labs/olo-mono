/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphMergeResult;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphValidationResult;

/**
 * Validates and merges model-produced subgraph JSON into the active workflow graph.
 */
public interface DynamicSubgraphMerger {

    DynamicSubgraphValidationResult validate(String rawJson);

    DynamicSubgraphMergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            String validatedJson);

    int readRetryCount(WorkflowRuntimeVariables variables);

    void incrementRetryCount(WorkflowRuntimeVariables variables);

    void resetRetryCount(WorkflowRuntimeVariables variables);
}
