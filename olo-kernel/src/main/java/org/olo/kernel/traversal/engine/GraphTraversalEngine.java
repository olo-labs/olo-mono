/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.engine;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.TraversalResult;

/**
 * Step-based graph traversal used by synchronous {@link org.olo.kernel.traversal.GraphTraverser}
 * and Temporal per-node activities.
 */
public interface GraphTraversalEngine {

    TraversalResult traverse(KernelRuntimeContext context);

    KernelExecutionSnapshot executeSingleStep(KernelExecutionSnapshot snapshot);
}
