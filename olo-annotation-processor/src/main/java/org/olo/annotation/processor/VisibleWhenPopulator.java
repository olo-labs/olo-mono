/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parses {@code key=value} visibility conditions from annotation arrays. */
final class VisibleWhenPopulator {

    private VisibleWhenPopulator() {}

    static Map<String, String> parse(String[] entries) {
        if (entries == null || entries.length == 0) {
            return null;
        }
        Map<String, String> conditions = new LinkedHashMap<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException(
                        "visibleWhen entry must be key=value, got: " + entry);
            }
            String key = entry.substring(0, separator).trim();
            String value = entry.substring(separator + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                throw new IllegalArgumentException(
                        "visibleWhen entry must be key=value, got: " + entry);
            }
            conditions.put(key, value);
        }
        return conditions.isEmpty() ? null : conditions;
    }
}
