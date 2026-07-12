/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.snapshot.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defensive copies for snapshot variable and output maps.
 */
public final class SnapshotMapSupport {

    private SnapshotMapSupport() {
    }

    public static <K, V> Map<K, V> copyMapAllowingNullValues(Map<K, V> source) {
        Objects.requireNonNull(source, "source");
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
