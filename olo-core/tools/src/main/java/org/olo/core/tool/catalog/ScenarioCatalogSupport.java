/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.catalog;

import org.olo.core.tool.observability.ObservabilitySupport;
import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Thin facade for reading JSON catalog fixtures from {@code demo-data/} for scenario planner tools.
 */
public final class ScenarioCatalogSupport {

    private ScenarioCatalogSupport() {
    }

    public static List<Map<String, Object>> readCatalog(
            String catalogFolder,
            ToolRequest request,
            String queryArgument,
            String topicArgument) throws IOException {
        Path folder = resolveFolder(catalogFolder);
        String query = string(request.arguments(), queryArgument,
                string(request.arguments(), "q", ""));
        String topic = string(request.arguments(), topicArgument, "");

        CatalogFolderReader.CatalogReadResult result = CatalogFolderReader.read(folder, query, topic);
        List<Map<String, Object>> entries = result.entries();
        List<Map<String, Object>> catalog = result.catalog();

        if (entries.isEmpty() && !catalog.isEmpty()) {
            entries.add(CatalogFallbackSelector.pickBestFallback(catalog, query, topic));
        }
        if (entries.isEmpty()) {
            throw new IOException("No catalog entries matched query in folder: " + folder);
        }
        int limit = parseLimit(request);
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public static Path resolveFolder(String catalogFolder) {
        return ObservabilitySupport.resolveDataFolderPath(catalogFolder);
    }

    private static int parseLimit(ToolRequest request) {
        Object raw = request.arguments().get("limit");
        if (raw == null) {
            return 5;
        }
        int limit = Integer.parseInt(String.valueOf(raw));
        return Math.max(1, limit);
    }

    private static String string(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || map.isEmpty()) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }
}
