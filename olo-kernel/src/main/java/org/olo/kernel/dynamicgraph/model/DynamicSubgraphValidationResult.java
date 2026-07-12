/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph.model;

public record DynamicSubgraphValidationResult(boolean valid, String normalizedJson, String message) {

    public static DynamicSubgraphValidationResult valid(String normalizedJson) {
        return new DynamicSubgraphValidationResult(true, normalizedJson, null);
    }

    public static DynamicSubgraphValidationResult invalid(String message) {
        return new DynamicSubgraphValidationResult(false, null, message);
    }
}
