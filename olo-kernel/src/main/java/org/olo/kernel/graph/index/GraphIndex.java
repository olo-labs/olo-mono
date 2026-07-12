/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.graph.index;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Indexed view of a workflow graph for traversal.
 */
public interface GraphIndex {

    Optional<NodeDefinition> findNode(String nodeId);

    List<EdgeDefinition> outgoingEdges(String nodeId);

    List<NodeDefinition> nodes();
}
