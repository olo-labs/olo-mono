/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.agenttool;

import org.olo.definition.configuration.agenttool.impl.AgentToolExecutionWorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Programmatic builder for the {@code agent} workflow preset with strict JSON tool-call planning.
 */
public final class AgentToolExecutionDefinitions {

    public static final String WORKFLOW_ID = "agent";
    public static final String CALCULATOR_NODE_ID = "calculator";
    public static final String CPU_USAGE_NODE_ID = "cpu-usage";
    public static final String CALCULATOR_TOOL_ID = "olo-core:calculator";
    public static final String CPU_USAGE_TOOL_ID = "olo-core:cpu-usage";
    public static final String RESTART_CONTAINER_NODE_ID = "restart-container";
    public static final String RESTART_CONTAINER_TOOL_ID = "olo-core:restart-container";

    private AgentToolExecutionDefinitions() {
    }

    public static WorkflowDefinition agent() {
        return AgentToolExecutionWorkflowBuilder.build();
    }
}
