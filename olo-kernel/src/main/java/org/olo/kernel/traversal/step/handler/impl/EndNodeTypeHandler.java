/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.spi.node.NodeResult;

import java.util.Map;

public final class EndNodeTypeHandler implements NodeTypeHandler {

    @Override
    public boolean supports(String nodeType) {
        return NodeType.END.name().equals(nodeType);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        return NodeResult.completed(Map.of());
    }
}
