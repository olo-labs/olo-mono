/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.catalog;

import java.util.List;
import java.util.Map;

import static org.olo.core.tool.catalog.CatalogSearchNormalization.combinedSearch;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.filenameStemFromSource;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.flatten;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.isSignificantToken;
import static org.olo.core.tool.catalog.CatalogSearchNormalization.normalize;

/**
 * Picks the best catalog entry when no explicit query match is found.
 */
final class CatalogFallbackSelector {

    private CatalogFallbackSelector() {
    }

    static Map<String, Object> pickBestFallback(List<Map<String, Object>> catalog, String query, String topic) {
        String search = normalize(combinedSearch(query, topic));
        Map<String, Object> best = catalog.getFirst();
        int bestScore = scoreEntry(best, search);
        for (Map<String, Object> entry : catalog) {
            int score = scoreEntry(entry, search);
            if (score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        return best;
    }

    /** Counts how many significant search tokens appear in the entry haystack. */
    static int scoreEntry(Map<String, Object> entry, String normalizedSearch) {
        if (normalizedSearch.isBlank()) {
            return 0;
        }
        String haystack = flatten(entry) + ' ' + filenameStemFromSource(entry);
        int score = 0;
        for (String token : normalizedSearch.split("\\s+")) {
            if (isSignificantToken(token) && haystack.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
