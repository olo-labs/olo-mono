/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.model;

import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Resolves the model provider and call settings from a workflow graph.
 */
public interface ModelProviderResolver {

    ResolvedModelCall resolve(WorkflowDefinition graph);
}
