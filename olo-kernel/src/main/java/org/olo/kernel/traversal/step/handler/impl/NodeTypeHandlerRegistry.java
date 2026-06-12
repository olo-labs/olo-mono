package org.olo.kernel.traversal.step.handler.impl;

import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;

import java.util.List;
import java.util.Objects;

public final class NodeTypeHandlerRegistry {

    private final List<NodeTypeHandler> handlers;

    public NodeTypeHandlerRegistry(List<NodeTypeHandler> handlers) {
        this.handlers = List.copyOf(Objects.requireNonNull(handlers, "handlers"));
    }

    public NodeTypeHandler resolve(String nodeType) {
        for (NodeTypeHandler handler : handlers) {
            if (handler.supports(nodeType)) {
                return handler;
            }
        }
        throw new KernelException("no node handler registered for type: " + nodeType);
    }
}
