package org.olo.kernel.graph.visit;

import org.olo.kernel.graph.index.GraphIndex;

import java.util.Optional;

/**
 * Resolves the next node to visit during linear graph traversal.
 */
public interface NextNodeResolver {

    Optional<String> resolveNext(GraphIndex index, String currentNodeId);
}
