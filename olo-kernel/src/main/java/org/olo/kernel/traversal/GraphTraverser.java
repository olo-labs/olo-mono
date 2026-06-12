package org.olo.kernel.traversal;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Traverses the workflow graph in {@link KernelRuntimeContext} and mutates runtime variables.
 */
public interface GraphTraverser {

    TraversalResult traverse(KernelRuntimeContext context);
}
