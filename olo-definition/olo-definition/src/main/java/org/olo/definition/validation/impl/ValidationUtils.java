/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared helpers used across workflow validation impl classes.
 */
final class ValidationUtils {

    private ValidationUtils() {
    }

    /** True when the value is null or contains only whitespace. */
    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Increments a per-node, per-port connection counter used for min/max connection checks. */
    static void incrementCount(Map<String, Map<String, Integer>> counts, String nodeId, String portId) {
        counts.computeIfAbsent(nodeId, ignored -> new HashMap<>())
                .merge(portId, 1, Integer::sum);
    }
}
