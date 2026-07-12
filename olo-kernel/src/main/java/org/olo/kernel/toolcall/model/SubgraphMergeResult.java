/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.model;

import java.util.List;

public record SubgraphMergeResult(org.olo.definition.workflow.WorkflowDefinition graph, String entryNodeId) {
}
