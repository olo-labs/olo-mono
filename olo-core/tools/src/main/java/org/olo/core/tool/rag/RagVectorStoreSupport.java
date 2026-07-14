/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * File-backed vector index for RAG ingest demos ({@code driver: file-json}).
 * Production deployments can swap this for pgvector/chroma via extension configuration.
 */
public final class RagVectorStoreSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String INDEX_FILE = ".rag-index.json";
    private static final int DEFAULT_VECTOR_SIZE = 384;

    private RagVectorStoreSupport() {
    }

    public record IngestResult(
            String capabilitySource,
            int filesProcessed,
            int chunksIndexed,
            String indexPath,
            List<Map<String, Object>> fileResults) {
    }

    public record SearchResult(
            String capabilitySource,
            String query,
            int hits,
            String ragContext,
            List<Map<String, Object>> matches) {
    }

    public static IngestResult ingestDocuments(
            Path uploadBaseDir,
            Path vectorIndexDir,
            String capabilitySource,
            List<String> fileNames,
            int chunkSize,
            Map<String, Object> extensionConfig) throws IOException {

        String source = safeSegment(capabilitySource);
        Path sourceUploadDir = uploadBaseDir.resolve(source);
        if (!Files.isDirectory(sourceUploadDir)) {
            throw new IOException("Upload directory not found for capability source: " + capabilitySource);
        }

        List<String> targets = fileNames == null || fileNames.isEmpty()
                ? listUploadFiles(sourceUploadDir)
                : fileNames;

        boolean qdrant = isQdrant(extensionConfig);
        Path indexFile = null;
        List<Map<String, Object>> existing = new ArrayList<>();
        if (!qdrant) {
            Path indexRoot = resolveIndexRoot(vectorIndexDir, extensionConfig);
            Files.createDirectories(indexRoot);
            indexFile = indexRoot.resolve(source).resolve(INDEX_FILE);
            Files.createDirectories(indexFile.getParent());
            existing = readIndex(indexFile);
        }
        List<Map<String, Object>> appended = new ArrayList<>();
        List<Map<String, Object>> fileResults = new ArrayList<>();
        int filesProcessed = 0;
        int totalChunks = 0;

        for (String fileName : targets) {
            Path file = sourceUploadDir.resolve(safeFileName(fileName));
            if (!Files.isRegularFile(file)) {
                fileResults.add(Map.of(
                        "fileName", safeFileName(fileName),
                        "status", "SKIPPED",
                        "reason", "file not found"));
                continue;
            }
            String text;
            try {
                text = readDocumentText(file);
            } catch (MalformedInputException e) {
                fileResults.add(Map.of(
                        "fileName", safeFileName(fileName),
                        "status", "SKIPPED",
                        "reason", "file is not valid UTF-8 text"));
                continue;
            } catch (IOException e) {
                fileResults.add(Map.of(
                        "fileName", safeFileName(fileName),
                        "status", "SKIPPED",
                        "reason", "file could not be read: " + e.getMessage()));
                continue;
            }
            List<String> chunks = chunkText(text, Math.max(128, chunkSize));
            int seq = 0;
            int fileChunkCount = 0;
            for (String chunk : chunks) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", source + ":" + safeFileName(fileName) + ":" + seq);
                entry.put("capabilitySource", capabilitySource);
                entry.put("fileName", safeFileName(fileName));
                entry.put("sequence", seq);
                entry.put("text", chunk);
                entry.put("indexedAt", System.currentTimeMillis());
                appended.add(entry);
                seq++;
                fileChunkCount++;
            }
            filesProcessed++;
            totalChunks += fileChunkCount;
            fileResults.add(Map.of(
                    "fileName", safeFileName(fileName),
                    "status", "INDEXED",
                    "chunksIndexed", fileChunkCount));
        }

        if (qdrant) {
            String indexPath = upsertQdrant(extensionConfig, capabilitySource, source, appended);
            return new IngestResult(capabilitySource, filesProcessed, totalChunks, indexPath, fileResults);
        }

        List<Map<String, Object>> merged = new ArrayList<>(existing);
        merged.addAll(appended);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), merged);

        return new IngestResult(capabilitySource, filesProcessed, totalChunks, indexFile.toString(), fileResults);
    }

    private static String readDocumentText(Path file) throws IOException {
        if (file.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file.toFile())) {
                return new PDFTextStripper().getText(document);
            }
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public static SearchResult search(
            Path vectorIndexDir,
            String capabilitySource,
            String query,
            int topK,
            double scoreThreshold,
            Map<String, Object> extensionConfig) throws IOException {

        if (isQdrant(extensionConfig)) {
            return searchQdrant(extensionConfig, capabilitySource, query, topK, scoreThreshold);
        }

        String source = safeSegment(capabilitySource);
        Path indexRoot = resolveIndexRoot(vectorIndexDir, extensionConfig);
        Path indexFile = indexRoot.resolve(source).resolve(INDEX_FILE);
        List<Map<String, Object>> index = readIndex(indexFile);
        if (index.isEmpty()) {
            return new SearchResult(capabilitySource, query, 0, "", List.of());
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) {
            return new SearchResult(capabilitySource, query, 0, "", List.of());
        }

        List<String> terms = tokenize(normalizedQuery);
        List<ScoredChunk> scored = new ArrayList<>();
        for (Map<String, Object> entry : index) {
            String text = String.valueOf(entry.getOrDefault("text", ""));
            double score = scoreText(text.toLowerCase(), terms);
            if (score >= scoreThreshold) {
                scored.add(new ScoredChunk(score, entry));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int limit = Math.max(1, topK);
        List<Map<String, Object>> matches = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        int rank = 0;
        for (ScoredChunk hit : scored) {
            if (rank >= limit) {
                break;
            }
            Map<String, Object> entry = hit.entry;
            matches.add(Map.of(
                    "fileName", String.valueOf(entry.getOrDefault("fileName", "")),
                    "sequence", entry.getOrDefault("sequence", 0),
                    "score", hit.score,
                    "text", String.valueOf(entry.getOrDefault("text", ""))));
            if (!context.isEmpty()) {
                context.append("\n\n");
            }
            context.append("[")
                    .append(entry.getOrDefault("fileName", "doc"))
                    .append(" #")
                    .append(entry.getOrDefault("sequence", 0))
                    .append("]\n")
                    .append(entry.getOrDefault("text", ""));
            rank++;
        }

        return new SearchResult(capabilitySource, query, matches.size(), context.toString(), matches);
    }

    private record ScoredChunk(double score, Map<String, Object> entry) {
    }

    private static List<String> tokenize(String query) {
        String[] parts = query.split("[^a-zA-Z0-9]+");
        List<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 2) {
                terms.add(part);
            }
        }
        return terms;
    }

    private static double scoreText(String text, List<String> terms) {
        if (terms.isEmpty() || text.isBlank()) {
            return 0.0;
        }
        int hits = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                hits++;
            }
        }
        return (double) hits / terms.size();
    }

    private static boolean isQdrant(Map<String, Object> extensionConfig) {
        Object driver = extensionConfig == null ? null : extensionConfig.get("driver");
        return driver != null && "qdrant".equalsIgnoreCase(String.valueOf(driver).trim());
    }

    private static String upsertQdrant(
            Map<String, Object> extensionConfig,
            String capabilitySource,
            String source,
            List<Map<String, Object>> entries) throws IOException {
        String baseUrl = qdrantBaseUrl(extensionConfig);
        String collection = qdrantCollection(extensionConfig, source);
        int vectorSize = qdrantVectorSize(extensionConfig);
        ensureQdrantCollection(baseUrl, collection, vectorSize, extensionConfig);
        if (entries.isEmpty()) {
            return baseUrl + "/collections/" + collection;
        }

        List<Map<String, Object>> points = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            points.add(Map.of(
                    "id", qdrantPointId(entry),
                    "vector", embed(String.valueOf(entry.getOrDefault("text", "")), vectorSize),
                    "payload", entry));
        }
        sendQdrant(
                "PUT",
                baseUrl + "/collections/" + collection + "/points?wait=true",
                Map.of("points", points),
                extensionConfig);
        return baseUrl + "/collections/" + collection + "#" + capabilitySource;
    }

    private static SearchResult searchQdrant(
            Map<String, Object> extensionConfig,
            String capabilitySource,
            String query,
            int topK,
            double scoreThreshold) throws IOException {
        String baseUrl = qdrantBaseUrl(extensionConfig);
        String collection = qdrantCollection(extensionConfig, safeSegment(capabilitySource));
        int vectorSize = qdrantVectorSize(extensionConfig);
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("vector", embed(query, vectorSize));
        requestBody.put("limit", Math.max(1, topK));
        requestBody.put("with_payload", true);
        requestBody.put("score_threshold", scoreThreshold);
        requestBody.put("filter", Map.of(
                "must", List.of(Map.of(
                        "key", "capabilitySource",
                        "match", Map.of("value", capabilitySource)))));

        JsonNode response = sendQdrant(
                "POST",
                baseUrl + "/collections/" + collection + "/points/search",
                requestBody,
                extensionConfig);
        JsonNode results = response.path("result");
        if (!results.isArray() || results.isEmpty()) {
            return new SearchResult(capabilitySource, query, 0, "", List.of());
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (JsonNode hit : results) {
            JsonNode payload = hit.path("payload");
            String fileName = payload.path("fileName").asText("");
            int sequence = payload.path("sequence").asInt(0);
            String text = payload.path("text").asText("");
            double score = hit.path("score").asDouble(0.0);
            matches.add(Map.of(
                    "fileName", fileName,
                    "sequence", sequence,
                    "score", score,
                    "text", text));
            if (!context.isEmpty()) {
                context.append("\n\n");
            }
            context.append("[")
                    .append(fileName.isBlank() ? "doc" : fileName)
                    .append(" #")
                    .append(sequence)
                    .append("]\n")
                    .append(text);
        }
        return new SearchResult(capabilitySource, query, matches.size(), context.toString(), matches);
    }

    private static void ensureQdrantCollection(
            String baseUrl,
            String collection,
            int vectorSize,
            Map<String, Object> extensionConfig) throws IOException {
        try {
            sendQdrant(
                    "PUT",
                    baseUrl + "/collections/" + collection,
                    Map.of("vectors", Map.of(
                            "size", vectorSize,
                            "distance", qdrantDistance(extensionConfig))),
                    extensionConfig);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Collection `")
                    && e.getMessage().contains("already exists")) {
                return;
            }
            throw e;
        }
    }

    private static JsonNode sendQdrant(
            String method,
            String url,
            Map<String, Object> body,
            Map<String, Object> extensionConfig) throws IOException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            Object apiKey = extensionConfig == null ? null : extensionConfig.get("apiKey");
            if (apiKey != null && !String.valueOf(apiKey).isBlank()) {
                builder.header("api-key", String.valueOf(apiKey));
            }
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Qdrant request failed (" + response.statusCode() + "): " + response.body());
            }
            return response.body().isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Qdrant request interrupted", e);
        }
    }

    private static String qdrantBaseUrl(Map<String, Object> extensionConfig) {
        Object ref = extensionConfig == null ? null : extensionConfig.get("connectionRef");
        if (ref == null || String.valueOf(ref).isBlank()) {
            ref = extensionConfig == null ? null : extensionConfig.get("url");
        }
        String value = ref == null || String.valueOf(ref).isBlank()
                ? "http://localhost:6333"
                : String.valueOf(ref).trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String qdrantCollection(Map<String, Object> extensionConfig, String fallback) {
        Object collection = extensionConfig == null ? null : extensionConfig.get("collection");
        if (collection == null || String.valueOf(collection).isBlank()) {
            collection = extensionConfig == null ? null : extensionConfig.get("table");
        }
        return safeSegment(collection == null || String.valueOf(collection).isBlank()
                ? fallback
                : String.valueOf(collection));
    }

    private static int qdrantVectorSize(Map<String, Object> extensionConfig) {
        Object raw = extensionConfig == null ? null : extensionConfig.get("vectorSize");
        if (raw instanceof Number n) {
            return Math.max(16, n.intValue());
        }
        if (raw != null) {
            try {
                return Math.max(16, Integer.parseInt(String.valueOf(raw)));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return DEFAULT_VECTOR_SIZE;
    }

    private static String qdrantDistance(Map<String, Object> extensionConfig) {
        Object raw = extensionConfig == null ? null : extensionConfig.get("distance");
        return raw == null || String.valueOf(raw).isBlank() ? "Cosine" : String.valueOf(raw).trim();
    }

    private static String qdrantPointId(Map<String, Object> entry) {
        String raw = String.valueOf(entry.getOrDefault("id", UUID.randomUUID().toString()));
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static List<Double> embed(String text, int vectorSize) {
        double[] values = new double[vectorSize];
        for (String token : tokenize(text == null ? "" : text.toLowerCase())) {
            byte[] digest = sha256(token);
            int bucket = Math.floorMod(ByteBuffer.wrap(digest, 0, Integer.BYTES).getInt(), vectorSize);
            values[bucket] += (digest[4] & 1) == 0 ? 1.0 : -1.0;
        }
        double norm = 0.0;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> out = new ArrayList<>(vectorSize);
        for (double value : values) {
            out.add(norm == 0.0 ? 0.0 : value / norm);
        }
        return out;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }
    private static Path resolveIndexRoot(Path vectorIndexDir, Map<String, Object> extensionConfig) {
        if (extensionConfig != null) {
            Object connectionRef = extensionConfig.get("connectionRef");
            if (connectionRef instanceof String ref && !ref.isBlank()) {
                String trimmed = ref.trim();
                if (trimmed.startsWith("${env:") && trimmed.endsWith("}")) {
                    String envKey = trimmed.substring("${env:".length(), trimmed.length() - 1).trim();
                    String fromEnv = System.getenv(envKey);
                    if (fromEnv != null && !fromEnv.isBlank()) {
                        return Paths.get(fromEnv).toAbsolutePath().normalize();
                    }
                } else if (!trimmed.contains("${")) {
                    return Paths.get(trimmed).toAbsolutePath().normalize();
                }
            }
        }
        return vectorIndexDir.toAbsolutePath().normalize();
    }

    private static List<String> listUploadFiles(Path sourceDir) throws IOException {
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(sourceDir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> !INDEX_FILE.equals(n))
                    .forEach(names::add);
        }
        return names;
    }

    private static List<Map<String, Object>> readIndex(Path indexFile) throws IOException {
        if (!Files.isRegularFile(indexFile)) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(indexFile.toFile(), new TypeReference<>() {});
    }

    static List<String> chunkText(String text, int chunkSize) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + chunkSize);
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt > start + chunkSize / 2) {
                    end = breakAt;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            start = end;
        }
        return chunks;
    }

    static String safeSegment(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            return "_";
        }
        return t.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "unnamed.bin";
        }
        Path p = Paths.get(original).getFileName();
        String n = p == null ? original : p.toString();
        if (n.contains("..") || n.indexOf('/') >= 0 || n.indexOf('\\') >= 0) {
            return "invalid-name";
        }
        return n.isBlank() ? "unnamed.bin" : n;
    }

    public static Map<String, Object> extensionConfigFrom(Map<String, Object> toolConfiguration) {
        if (toolConfiguration == null || toolConfiguration.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : List.of("driver", "connectionRef", "url", "table", "extensionRef", "collection", "vectorSize", "distance", "apiKey")) {
            Object value = toolConfiguration.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                out.put(key, value);
            }
        }
        return out;
    }

    public static String readExtensionRef(Map<String, Object> toolConfiguration) {
        Object ref = toolConfiguration == null ? null : toolConfiguration.get("extensionRef");
        return ref == null ? "pgvector-store" : String.valueOf(ref);
    }

    public static int readChunkSize(Map<String, Object> toolConfiguration, Map<String, Object> arguments) {
        Object fromArgs = arguments == null ? null : arguments.get("chunkSize");
        if (fromArgs instanceof Number n) {
            return n.intValue();
        }
        if (fromArgs != null) {
            try {
                return Integer.parseInt(String.valueOf(fromArgs));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        Object fromConfig = toolConfiguration == null ? null : toolConfiguration.get("chunkSize");
        if (fromConfig instanceof Number n) {
            return n.intValue();
        }
        if (fromConfig != null) {
            try {
                return Integer.parseInt(String.valueOf(fromConfig));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 512;
    }

    public static List<String> readFileNames(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return List.of();
        }
        Object raw = arguments.get("fileNames");
        if (raw instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        Object csv = arguments.get("fileNamesCsv");
        if (csv != null && !String.valueOf(csv).isBlank()) {
            String[] parts = String.valueOf(csv).split(",");
            List<String> names = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
            return names;
        }
        Object single = arguments.get("fileName");
        if (single != null && !String.valueOf(single).isBlank()) {
            return List.of(String.valueOf(single).trim());
        }
        return List.of();
    }

    public static int readTopK(Map<String, Object> toolConfiguration, Map<String, Object> arguments) {
        return readNumber(toolConfiguration, arguments, "topK", 5);
    }

    public static double readScoreThreshold(Map<String, Object> toolConfiguration, Map<String, Object> arguments) {
        int asInt = readNumber(toolConfiguration, arguments, "scoreThreshold", 0);
        if (asInt > 0 && asInt <= 1) {
            return asInt;
        }
        Object fromArgs = arguments == null ? null : arguments.get("scoreThreshold");
        if (fromArgs instanceof Number n) {
            return n.doubleValue();
        }
        Object fromConfig = toolConfiguration == null ? null : toolConfiguration.get("scoreThreshold");
        if (fromConfig instanceof Number n) {
            return n.doubleValue();
        }
        if (fromConfig != null) {
            try {
                return Double.parseDouble(String.valueOf(fromConfig));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0.25;
    }

    private static int readNumber(
            Map<String, Object> toolConfiguration,
            Map<String, Object> arguments,
            String key,
            int fallback) {
        Object fromArgs = arguments == null ? null : arguments.get(key);
        if (fromArgs instanceof Number n) {
            return n.intValue();
        }
        if (fromArgs != null) {
            try {
                return Integer.parseInt(String.valueOf(fromArgs));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        Object fromConfig = toolConfiguration == null ? null : toolConfiguration.get(key);
        if (fromConfig instanceof Number n) {
            return n.intValue();
        }
        if (fromConfig != null) {
            try {
                return Integer.parseInt(String.valueOf(fromConfig));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
