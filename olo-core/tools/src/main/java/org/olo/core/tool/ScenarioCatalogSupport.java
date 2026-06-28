package org.olo.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads JSON catalog fixtures from {@code demo-data/} for scenario planner tools.
 */
final class ScenarioCatalogSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ScenarioCatalogSupport() {
    }

    static List<Map<String, Object>> readCatalog(
            String catalogFolder,
            ToolRequest request,
            String queryArgument,
            String topicArgument) throws IOException {
        Path folder = resolveFolder(catalogFolder);
        if (!Files.isDirectory(folder)) {
            throw new IOException("Catalog folder does not exist: " + folder);
        }
        String query = ToolArgs.string(request.arguments(), queryArgument,
                ToolArgs.string(request.arguments(), "q", ""));
        String topic = ToolArgs.string(request.arguments(), topicArgument, "");
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
        if (entries.isEmpty() && !catalog.isEmpty()) {
            entries.add(pickBestFallback(catalog, query, topic));
        }
        if (entries.isEmpty()) {
            throw new IOException("No catalog entries matched query in folder: " + folder);
        }
        int limit = parseLimit(request);
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    static Path resolveFolder(String catalogFolder) {
        return ObservabilitySupport.resolveDataFolderPath(catalogFolder);
    }

    private static Map<String, Object> enrich(Map<String, Object> document, Path path) {
        Map<String, Object> enriched = new LinkedHashMap<>(document);
        enriched.putIfAbsent("sourceFile", path.getFileName().toString());
        return enriched;
    }

    private static boolean matches(Map<String, Object> document, Path path, String query, String topic) {
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
                if (keyword != null && topicMatchesSearch(normalize(String.valueOf(keyword)), normalizedSearch)) {
                    return true;
                }
            }
        }

        return searchTokensMatchHaystack(normalizedSearch, haystack);
    }

    private static Map<String, Object> pickBestFallback(
            List<Map<String, Object>> catalog, String query, String topic) {
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

    private static int scoreEntry(Map<String, Object> entry, String normalizedSearch) {
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

    private static String filenameStemFromSource(Map<String, Object> entry) {
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

    private static boolean topicMatchesSearch(String docTopic, String normalizedSearch) {
        if (docTopic.isBlank()) {
            return false;
        }
        if (normalizedSearch.contains(docTopic) || docTopic.contains(normalizedSearch)) {
            return true;
        }
        return coversTopicTokens(normalizedSearch, docTopic);
    }

    /** True when at least two significant search tokens (or all, if fewer) appear in haystack. */
    private static boolean searchTokensMatchHaystack(String normalizedSearch, String haystack) {
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

    private static String combinedSearch(String query, String topic) {
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

    private static String filenameStem(Path path) {
        String name = path.getFileName().toString();
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return normalize(name.replace('-', ' ').replace('_', ' '));
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
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

    private static boolean isSignificantToken(String token) {
        return token.length() >= 3 || "olo".equals(token);
    }

    private static String flatten(Map<String, Object> document) {
        StringBuilder builder = new StringBuilder();
        for (Object value : document.values()) {
            if (value != null) {
                builder.append(String.valueOf(value).toLowerCase(Locale.ROOT)).append(' ');
            }
        }
        return builder.toString();
    }

    private static int parseLimit(ToolRequest request) {
        Object raw = request.arguments().get("limit");
        if (raw == null) {
            return 5;
        }
        int limit = Integer.parseInt(String.valueOf(raw));
        return Math.max(1, limit);
    }
}
