/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.ToolCallRetryVariables;

public final class DefaultToolCallRetryVariables implements ToolCallRetryVariables {

    @Override
    public int readRetryCount(WorkflowRuntimeVariables variables) {
        Object raw = variables.get(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE);
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
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
}
