/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.graph.start;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.graph.index.GraphIndex;

import java.util.Optional;

/**
 * Locates the workflow entry node for graph traversal.
 */
public interface StartNodeResolver {

    Optional<NodeDefinition> resolve(GraphIndex index);
}
