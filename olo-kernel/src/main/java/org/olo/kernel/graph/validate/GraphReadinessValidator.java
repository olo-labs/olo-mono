package org.olo.kernel.graph.validate;

import org.olo.kernel.graph.index.GraphIndex;

/**
 * Validates that a workflow graph can be traversed.
 */
public interface GraphReadinessValidator {

    boolean isReady(GraphIndex index);
}
