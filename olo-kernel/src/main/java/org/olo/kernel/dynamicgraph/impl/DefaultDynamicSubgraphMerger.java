/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.dynamicgraph.impl;

import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.dynamicgraph.DynamicSubgraphMerger;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphMergeResult;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphValidationResult;

public final class DefaultDynamicSubgraphMerger implements DynamicSubgraphMerger {

    @Override
    public DynamicSubgraphValidationResult validate(String rawJson) {
        return DynamicSubgraphJsonValidator.validate(rawJson);
    }

    @Override
    public DynamicSubgraphMergeResult merge(
            org.olo.definition.workflow.WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            String validatedJson) {
        return DynamicSubgraphGraphMerger.merge(graph, plannerNodeId, continueNodeId, validatedJson);
    }

    @Override
    public int readRetryCount(WorkflowRuntimeVariables variables) {
        Object raw = variables.get(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    @Override
    public void incrementRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE, readRetryCount(variables) + 1);
    }

    @Override
    public void resetRetryCount(WorkflowRuntimeVariables variables) {
        variables.set(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE, 0);
    }
}
