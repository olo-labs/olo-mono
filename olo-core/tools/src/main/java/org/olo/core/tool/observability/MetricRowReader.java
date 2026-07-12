/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.observability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.olo.core.tool.observability.TimeRangeParser.TimeRange;
import static org.olo.core.tool.observability.TimeRangeParser.parseInstant;

/**
 * Reads CSV metric samples from a folder and filters them by time range.
 */
final class MetricRowReader {

    private MetricRowReader() {
    }

    static List<Map<String, Object>> readMetricRows(Path folder, TimeRange range) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IOException("Metric folder does not exist: " + folder);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Stream<Path> paths = Files.list(folder)) {
            List<Path> metricFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".csv") || name.endsWith(".json");
                    })
                    .sorted()
                    .toList();
            if (metricFiles.isEmpty()) {
                throw new IOException("No metric files found in folder: " + folder);
            }
            for (Path file : metricFiles) {
                rows.addAll(readMetricFile(file, range));
            }
        }
        rows.sort((left, right) -> Instant.parse(String.valueOf(left.get("timestamp")))
                .compareTo(Instant.parse(String.valueOf(right.get("timestamp")))));
        return rows;
    }

    private static List<Map<String, Object>> readMetricFile(Path file, TimeRange range) throws IOException {
        if (file.getFileName().toString().toLowerCase().endsWith(".csv")) {
            return readMetricCsv(file, range);
        }
        throw new IOException("Unsupported metric file type: " + file.getFileName());
    }

    private static List<Map<String, Object>> readMetricCsv(Path file, TimeRange range) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            return List.of();
        }
        String header = lines.getFirst().toLowerCase();
        if (!header.contains("timestamp") || !header.contains("value")) {
            throw new IOException("Metric CSV must include timestamp and value columns: " + file);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 2) {
                continue;
            }
            Instant timestamp = parseInstant(parts[0].trim(), "metric timestamp");
            if (!range.contains(timestamp)) {
                continue;
            }
            double value = Double.parseDouble(parts[1].trim());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", timestamp.toString());
            row.put("value", value);
            row.put("sourceFile", file.getFileName().toString());
            rows.add(row);
        }
        return rows;
    }
}
