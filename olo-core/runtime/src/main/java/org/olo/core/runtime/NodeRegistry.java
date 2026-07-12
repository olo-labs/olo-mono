/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.runtime;

import org.olo.core.node.CoreNodes;
import org.olo.spi.node.Node;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of {@link Node} implementations keyed by {@link Node#nodeType()}.
 */
public final class NodeRegistry {

    private final Map<String, Node> byType = new LinkedHashMap<>();

    public void register(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        byType.put(node.nodeType(), node);
    }

    public void registerAll(Collection<Node> nodes) {
        if (nodes != null) {
            nodes.forEach(this::register);
        }
    }

    public Optional<Node> find(String nodeType) {
        if (nodeType == null || nodeType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byType.get(nodeType));
    }

    public Map<String, Node> snapshot() {
        return Map.copyOf(byType);
    }

    public static NodeRegistry withDefaults() {
        NodeRegistry registry = new NodeRegistry();
        registry.registerAll(CoreNodes.all());
        return registry;
    }
}
