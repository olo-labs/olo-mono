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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.olo.core.tool.observability.TimeRangeParser.TimeRange;
import static org.olo.core.tool.observability.TimeRangeParser.parseInstant;

/**
 * Reads {@code .log} files from a folder and filters lines by timestamp.
 */
final class LogLineReader {

    private static final Pattern LOG_TIMESTAMP =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+");

    private LogLineReader() {
    }

    static List<String> readLogLines(Path folder, TimeRange range) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IOException("Log folder does not exist: " + folder);
        }
        List<String> matched = new ArrayList<>();
        try (Stream<Path> paths = Files.list(folder)) {
            List<Path> logFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".log"))
                    .sorted()
                    .toList();
            if (logFiles.isEmpty()) {
                throw new IOException("No .log files found in folder: " + folder);
            }
            for (Path file : logFiles) {
                for (String line : Files.readAllLines(file)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    Instant timestamp = parseLogTimestamp(line);
                    if (timestamp != null && range.contains(timestamp)) {
                        matched.add(line);
                    }
                }
            }
        }
        return matched;
    }

    private static Instant parseLogTimestamp(String line) {
        Matcher matcher = LOG_TIMESTAMP.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        try {
            return parseInstant(matcher.group(1), "log line");
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
