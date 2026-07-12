/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.extension;

import org.olo.spi.node.Node;

import java.util.Optional;

/**
 * Supplies {@link Node} implementations for a workflow node type.
 */
public interface NodeProvider extends ExtensionPoint {

    /**
     * Node type token this provider handles (e.g. {@code MODEL}, {@code TOOL}).
     */
    String nodeType();

    /**
     * Returns the node implementation for the type, if supported.
     */
    Optional<Node> getNode();
}
