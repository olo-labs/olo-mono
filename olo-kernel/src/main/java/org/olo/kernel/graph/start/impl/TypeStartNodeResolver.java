package org.olo.kernel.graph.start.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.start.StartNodeResolver;

import java.util.Optional;

public final class TypeStartNodeResolver implements StartNodeResolver {

    @Override
    public Optional<NodeDefinition> resolve(GraphIndex index) {
        for (NodeDefinition node : index.nodes()) {
            if (node != null && NodeType.START.name().equals(node.getType())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }
}
