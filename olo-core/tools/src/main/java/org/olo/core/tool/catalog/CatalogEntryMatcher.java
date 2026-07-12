/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.catalog;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.olo.core.tool.catalog.CatalogSearchNormalization.combinedSearch;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.filenameStem;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.flatten;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.isSignificantToken;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.normalize;

/**
 * Decides whether a catalog document matches the caller's query and topic.
 */
final class CatalogEntryMatcher {

    private CatalogEntryMatcher() {
    }

    static boolean matches(Map<String, Object> document, Path path, String query, String topic) {
        String search = combinedSearch(query, topic);
        if (search.isBlank()) {
            return true;
        }
        String normalizedSearch = normalize(search);
        String haystack = flatten(document) + ' ' + filenameStem(path);

        if (haystack.contains(normalizedSearch)) {
            return true;
        }

        Object rawTopic = document.get("topic");
        if (rawTopic != null) {
            String docTopic = normalize(String.valueOf(rawTopic));
            if (topicMatchesSearch(docTopic, normalizedSearch)) {
                return true;
            }
        }

        for (String field : List.of("destination", "origin", "region")) {
            Object value = document.get(field);
            if (value != null && topicMatchesSearch(normalize(String.valueOf(value)), normalizedSearch)) {
                return true;
            }
        }

        Object keywords = document.get("keywords");
        if (keywords instanceof List<?> keywordList) {
            for (Object keyword : keywordList) {
                if (keyword != null
                        && topicMatchesSearch(normalize(String.valueOf(keyword)), normalizedSearch)) {
                    return true;
                }
            }
        }

        return searchTokensMatchHaystack(normalizedSearch, haystack);
    }

    /** Checks substring overlap or token coverage between document topic and search text. */
    static boolean topicMatchesSearch(String docTopic, String normalizedSearch) {
        if (docTopic.isBlank()) {
            return false;
        }
        if (normalizedSearch.contains(docTopic) || docTopic.contains(normalizedSearch)) {
            return true;
        }
        return coversTopicTokens(normalizedSearch, docTopic);
    }

    /**
     * True when at least two significant search tokens (or all, if fewer) appear in haystack.
     */
    static boolean searchTokensMatchHaystack(String normalizedSearch, String haystack) {
        String[] searchTokens = normalizedSearch.split("\\s+");
        int required = 0;
        int matched = 0;
        for (String token : searchTokens) {
            if (!isSignificantToken(token)) {
                continue;
            }
            required++;
            if (haystack.contains(token)) {
                matched++;
            }
        }
        if (required == 0) {
            return false;
        }
        int threshold = Math.min(2, required);
        return matched >= threshold;
    }

    /** True when every significant token in {@code topic} appears in {@code search}. */
    private static boolean coversTopicTokens(String search, String topic) {
        String[] topicTokens = topic.split("\\s+");
        int required = 0;
        int matched = 0;
        for (String token : topicTokens) {
            if (!isSignificantToken(token)) {
                continue;
            }
            required++;
            if (search.contains(token)) {
                matched++;
            }
        }
        if (required == 0) {
            return false;
        }
        return matched >= required;
    }
}
