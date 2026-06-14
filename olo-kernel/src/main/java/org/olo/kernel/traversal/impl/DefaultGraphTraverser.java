package org.olo.kernel.traversal.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.GraphTraverser;
import org.olo.kernel.traversal.TraversalResult;
import org.olo.kernel.traversal.engine.GraphTraversalEngine;

import java.util.Objects;

/**
 * Walks the workflow graph by delegating to {@link GraphTraversalEngine}.
 */
public final class DefaultGraphTraverser implements GraphTraverser {

    private final GraphTraversalEngine engine;

    public DefaultGraphTraverser(GraphTraversalEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public TraversalResult traverse(KernelRuntimeContext context) {
        return engine.traverse(context);
    }
}
