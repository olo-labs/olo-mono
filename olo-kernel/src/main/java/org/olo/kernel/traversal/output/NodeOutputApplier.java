/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.output;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeResult;

/**
 * Applies node execution output back into the kernel runtime variable map.
 */
public interface NodeOutputApplier {

    void apply(KernelRuntimeContext context, NodeDefinition node, NodeResult result);
}
