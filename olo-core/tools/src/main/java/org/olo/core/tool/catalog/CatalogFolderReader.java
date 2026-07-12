/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.olo.core.tool.catalog.CatalogEntryMatcher.matches;

/**
 * Reads JSON catalog fixtures from a folder and filters them by query/topic.
 */
final class CatalogFolderReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CatalogFolderReader() {
    }

    /**
     * Loads every {@code .json} file in {@code folder}, returning matched entries and the
     * full catalog for fallback selection.
     */
    static CatalogReadResult read(Path folder, String query, String topic) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IOException("Catalog folder does not exist: " + folder);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        List<Map<String, Object>> catalog = new ArrayList<>();
        try (Stream<Path> paths = Files.list(folder)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(path -> {
                        try {
                            Map<String, Object> document = MAPPER.readValue(
                                    Files.readString(path), new TypeReference<>() {});
                            Map<String, Object> enriched = enrich(document, path);
                            catalog.add(enriched);
                            if (matches(document, path, query, topic)) {
                                entries.add(enriched);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to read catalog file: " + path, e);
                        }
                    });
        }
        return new CatalogReadResult(entries, catalog);
    }

    /** Adds {@code sourceFile} metadata when the document does not already include it. */
    private static Map<String, Object> enrich(Map<String, Object> document, Path path) {
        Map<String, Object> enriched = new LinkedHashMap<>(document);
        enriched.putIfAbsent("sourceFile", path.getFileName().toString());
        return enriched;
    }

    /** Matched entries plus the complete catalog loaded from disk. */
    record CatalogReadResult(List<Map<String, Object>> entries, List<Map<String, Object>> catalog) {
    }
}
