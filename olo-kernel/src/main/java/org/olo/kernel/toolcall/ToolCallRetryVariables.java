/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

public interface ToolCallRetryVariables {

    int readRetryCount(WorkflowRuntimeVariables variables);

    void incrementRetryCount(WorkflowRuntimeVariables variables);

    void resetRetryCount(WorkflowRuntimeVariables variables);

    void resetToolResults(WorkflowRuntimeVariables variables);
}
