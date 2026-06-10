package org.olo.spi.node;

import org.olo.spi.context.ExecutionContext;

/**
 * Runtime contract for a workflow node implementation.
 * <p>
 * Graph shape and configuration come from {@code olo-definition}; this interface is the execution boundary.
 */
public interface Node {

    /**
     * Node type token matching {@code NodeDefinition.type} (e.g. {@code MODEL}, {@code TOOL}, {@code HUMAN}).
     */
    String nodeType();

    /**
     * Executes the node for the given request within the shared execution context.
     */
    NodeResult execute(NodeRequest request, ExecutionContext context);
}
