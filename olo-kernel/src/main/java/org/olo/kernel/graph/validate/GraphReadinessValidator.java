/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.graph.validate;

import org.olo.kernel.graph.index.GraphIndex;

/**
 * Validates that a workflow graph can be traversed.
 */
public interface GraphReadinessValidator {

    boolean isReady(GraphIndex index);
}
