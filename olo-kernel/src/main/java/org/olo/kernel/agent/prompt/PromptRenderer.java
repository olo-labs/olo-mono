package org.olo.kernel.agent.prompt;

import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

/**
 * Renders a workflow prompt template using runtime variables.
 */
public interface PromptRenderer {

    String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables);
}
