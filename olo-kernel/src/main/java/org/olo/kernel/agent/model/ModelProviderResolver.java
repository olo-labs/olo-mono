package org.olo.kernel.agent.model;

import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Resolves the model provider and call settings from a workflow graph.
 */
public interface ModelProviderResolver {

    ResolvedModelCall resolve(WorkflowDefinition graph);
}
