/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Traverses the workflow graph in {@link KernelRuntimeContext} and mutates runtime variables.
 */
public interface GraphTraverser {

    TraversalResult traverse(KernelRuntimeContext context);
}
