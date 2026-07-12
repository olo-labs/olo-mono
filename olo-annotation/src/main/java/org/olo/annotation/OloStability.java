/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Extension maturity for Studio, marketplace, and compatibility checks.
 * <p>
 * Serialized in catalogs as lowercase ({@code stable}, {@code beta}, {@code experimental}).
 */
public enum OloStability {
    STABLE,
    BETA,
    EXPERIMENTAL
}
