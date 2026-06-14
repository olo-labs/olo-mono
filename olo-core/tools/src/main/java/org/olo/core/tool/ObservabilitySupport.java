package org.olo.core.tool;

import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ObservabilitySupport {

    static final String DEMO_DATA_ROOT_ENV = "OLO_DEMO_DATA_ROOT";

    private static final Pattern LOG_TIMESTAMP =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+");

    private ObservabilitySupport() {
    }

    static Path resolveDataFolder(ToolRequest request, String configurationKey, String defaultFolder) {
        String configured = ToolArgs.string(request.arguments(), configurationKey,
                ToolArgs.string(request.configuration(), configurationKey, defaultFolder));
        if (configured.isBlank()) {
            throw new IllegalArgumentException("dataFolder is required");
        }
        return resolveDataFolderPath(configured);
    }

    static Path resolveDataFolderPath(String configured) {
        Path path = Path.of(configured).normalize();
        if (path.isAbsolute()) {
            return path;
        }
        if (Files.isDirectory(path)) {
            return path.toAbsolutePath().normalize();
        }
        Path toolsDirectory = discoverToolsDirectory();
        if (toolsDirectory != null) {
            Path resolved = toolsDirectory.resolve(path).normalize();
            if (Files.isDirectory(resolved)) {
                return resolved;
            }
        }
        return path;
    }

    static Path discoverToolsDirectory() {
        Path override = readDemoDataRootOverride();
        if (override != null) {
            return override;
        }
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 12 && current != null; depth++) {
            if (Files.isDirectory(current.resolve("demo-data"))) {
                return current;
            }
            Path monoTools = current.resolve("olo-mono").resolve("olo-core").resolve("tools");
            if (Files.isDirectory(monoTools.resolve("demo-data"))) {
                return monoTools;
            }
            Path coreTools = current.resolve("olo-core").resolve("tools");
            if (Files.isDirectory(coreTools.resolve("demo-data"))) {
                return coreTools;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Path readDemoDataRootOverride() {
        String configured = System.getenv(DEMO_DATA_ROOT_ENV);
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty(DEMO_DATA_ROOT_ENV);
        }
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return Path.of(configured.trim());
    }

    static TimeRange parseTimeRange(ToolRequest request) {
        String startTime = ToolArgs.string(request.arguments(), "startTime",
                ToolArgs.string(request.configuration(), "startTime", ""));
        String endTime = ToolArgs.string(request.arguments(), "endTime",
                ToolArgs.string(request.configuration(), "endTime", ""));
        if (startTime.isBlank() || endTime.isBlank()) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        Instant start = parseInstant(startTime, "startTime");
        Instant end = parseInstant(endTime, "endTime");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endTime must be on or after startTime");
        }
        return new TimeRange(start, end);
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

    private static Instant parseInstant(String raw, String fieldName) {
        String text = raw.trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " timestamp: " + raw, e);
        }
    }

    record TimeRange(Instant start, Instant end) {
        boolean contains(Instant instant) {
            return !instant.isBefore(start) && !instant.isAfter(end);
        }
    }
}
