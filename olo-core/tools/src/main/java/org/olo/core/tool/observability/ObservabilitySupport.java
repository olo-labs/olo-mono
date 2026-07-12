/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.observability;

import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Thin facade for demo-data path resolution and observability fixture reads.
 */
public final class ObservabilitySupport {

    public static final String DEMO_DATA_ROOT_ENV = DataFolderResolver.DEMO_DATA_ROOT_ENV;

    private ObservabilitySupport() {
    }

    public static Path resolveDataFolder(ToolRequest request, String configurationKey, String defaultFolder) {
        String configured = string(request.arguments(), configurationKey,
                string(request.configuration(), configurationKey, defaultFolder));
        if (configured.isBlank()) {
            throw new IllegalArgumentException("dataFolder is required");
        }
        return DataFolderResolver.resolveDataFolderPath(configured);
    }

    public static Path resolveDataFolderPath(String configured) {
        return DataFolderResolver.resolveDataFolderPath(configured);
    }

    public static Path discoverToolsDirectory() {
        return DataFolderResolver.discoverToolsDirectory();
    }

    public static TimeRangeParser.TimeRange parseTimeRange(ToolRequest request) {
        String startTime = string(request.arguments(), "startTime",
                string(request.configuration(), "startTime", ""));
        String endTime = string(request.arguments(), "endTime",
                string(request.configuration(), "endTime", ""));
        return TimeRangeParser.parseTimeRange(startTime, endTime);
    }

    public static List<Map<String, Object>> readMetricRows(Path folder, TimeRangeParser.TimeRange range)
            throws IOException {
        return MetricRowReader.readMetricRows(folder, range);
    }

    public static List<String> readLogLines(Path folder, TimeRangeParser.TimeRange range) throws IOException {
        return LogLineReader.readLogLines(folder, range);
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
