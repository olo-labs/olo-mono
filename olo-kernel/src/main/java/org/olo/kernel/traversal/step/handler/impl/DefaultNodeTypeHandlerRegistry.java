/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.kernel.traversal.step.handler.NodeTypeHandlerRegistry;

import java.util.List;
import java.util.Objects;

public final class DefaultNodeTypeHandlerRegistry implements NodeTypeHandlerRegistry {

    private final List<NodeTypeHandler> handlers;

    public DefaultNodeTypeHandlerRegistry(List<NodeTypeHandler> handlers) {
        this.handlers = List.copyOf(Objects.requireNonNull(handlers, "handlers"));
    }

    @Override
    public NodeTypeHandler resolve(String nodeType) {
        for (NodeTypeHandler handler : handlers) {
            if (handler.supports(nodeType)) {
                return handler;
            }
        }
        throw new KernelException("no node handler registered for type: " + nodeType);
    }
}
