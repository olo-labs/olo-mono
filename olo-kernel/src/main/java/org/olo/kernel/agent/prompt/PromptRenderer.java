/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.prompt;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

/**
 * Renders a workflow prompt template using runtime variables.
 */
public interface PromptRenderer {

    String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables);

    String renderForNode(WorkflowDefinition graph, NodeDefinition node, WorkflowRuntimeVariables variables);
}
