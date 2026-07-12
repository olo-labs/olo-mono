/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph.model;

import org.olo.definition.workflow.WorkflowDefinition;

public record DynamicSubgraphMergeResult(WorkflowDefinition graph, String entryNodeId) {
}
