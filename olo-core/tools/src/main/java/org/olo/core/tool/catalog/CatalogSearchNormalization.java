/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.catalog;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes catalog search text and builds flattened haystacks for matching.
 */
final class CatalogSearchNormalization {

    private CatalogSearchNormalization() {
    }

    /** Lowercases, strips punctuation, and collapses whitespace for fuzzy matching. */
    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Concatenates all document values into a single searchable string. */
    static String flatten(Map<String, Object> document) {
        StringBuilder builder = new StringBuilder();
        for (Object value : document.values()) {
            if (value != null) {
                builder.append(String.valueOf(value).toLowerCase(Locale.ROOT)).append(' ');
            }
        }
        return builder.toString();
    }

    /** Merges topic and query arguments into one search phrase. */
    static String combinedSearch(String query, String topic) {
        StringBuilder builder = new StringBuilder();
        if (topic != null && !topic.isBlank()) {
            builder.append(topic.trim());
        }
        if (query != null && !query.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(query.trim());
        }
        return builder.toString();
    }

    /** Tokens shorter than three characters are ignored unless they are product keywords. */
    static boolean isSignificantToken(String token) {
        return token.length() >= 3 || "olo".equals(token);
    }

    /** Normalized filename stem without the {@code .json} suffix. */
    static String filenameStem(Path path) {
        String name = path.getFileName().toString();
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return normalize(name.replace('-', ' ').replace('_', ' '));
    }

    /** Normalized stem derived from an enriched entry's {@code sourceFile} field. */
    static String filenameStemFromSource(Map<String, Object> entry) {
        Object source = entry.get("sourceFile");
        if (source == null) {
            return "";
        }
        String name = String.valueOf(source);
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return normalize(name.replace('-', ' ').replace('_', ' '));
    }
}
