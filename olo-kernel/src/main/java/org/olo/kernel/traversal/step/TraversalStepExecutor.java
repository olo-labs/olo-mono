/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeResult;

/**
 * Executes one traversal step for a graph node.
 */
public interface TraversalStepExecutor {

    NodeResult execute(KernelRuntimeContext context, NodeDefinition node);

    default NodeResult execute(KernelRuntimeContext context, NodeDefinition node, int step) {
        return execute(context, node);
    }
}
