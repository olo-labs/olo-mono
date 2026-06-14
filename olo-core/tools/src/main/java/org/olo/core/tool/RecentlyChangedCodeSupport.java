package org.olo.core.tool;

import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class RecentlyChangedCodeSupport {

    private RecentlyChangedCodeSupport() {
    }

    static List<Map<String, Object>> readChanges(Path folder, int limit, String pullRequestNumber) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IOException("Changes folder does not exist: " + folder);
        }
        List<Path> files;
        try (Stream<Path> paths = Files.list(folder)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .filter(RecentlyChangedCodeSupport::isChangeFile)
                    .filter(path -> matchesPullRequest(path, pullRequestNumber))
                    .sorted(Comparator.comparing(RecentlyChangedCodeSupport::lastModified).reversed())
                    .limit(limit)
                    .toList();
        }
        if (files.isEmpty()) {
            throw new IOException("No change files found in folder: " + folder);
        }
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Path file : files) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("fileName", file.getFileName().toString());
            entry.put("lastModified", Files.getLastModifiedTime(file).toInstant().toString());
            entry.put("content", Files.readString(file));
            changes.add(entry);
        }
        return changes;
    }

    static int parseLimit(ToolRequest request) {
        Object raw = request.arguments().get("limit");
        if (raw == null) {
            raw = request.configuration().get("limit");
        }
        if (raw == null) {
            return 5;
        }
        int limit = Integer.parseInt(String.valueOf(raw));
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        return limit;
    }

    static String parsePullRequestNumber(ToolRequest request) {
        return ToolArgs.string(request.arguments(), "pullRequestNumber",
                ToolArgs.string(request.configuration(), "pullRequestNumber", ""));
    }

    private static boolean isChangeFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.equals("readme.md")) {
            return false;
        }
        return name.endsWith(".patch")
                || name.endsWith(".diff")
                || name.endsWith(".java")
                || name.endsWith(".json")
                || name.endsWith(".txt");
    }

    private static boolean matchesPullRequest(Path path, String pullRequestNumber) {
        if (pullRequestNumber == null || pullRequestNumber.isBlank()) {
            return true;
        }
        return path.getFileName().toString().contains(pullRequestNumber);
    }

    private static Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }
}
