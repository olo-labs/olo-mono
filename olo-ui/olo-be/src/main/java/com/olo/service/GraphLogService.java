/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package com.olo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.config.OloConfigurationProperties;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Structural audit metadata only; graph and planner payloads are never exposed. */
@Service
public class GraphLogService {
    private final OloConfigurationProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    public GraphLogService(OloConfigurationProperties properties) { this.properties = properties; }
    public Path logRoot() { return properties.resolvedLogDirectory().toAbsolutePath().normalize(); }
    public List<GraphLogSummary> listLogs() throws IOException {
        Path root = logRoot();
        if (!Files.isDirectory(root)) return List.of();
        List<GraphLogSummary> result = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).sorted(Comparator.reverseOrder()).toList()) {
                try { result.add(readMetadata(root.relativize(path).toString().replace('\\', '/'))); }
                catch (IOException | IllegalArgumentException ignored) { /* Never log parser payloads. */ }
            }
        }
        return List.copyOf(result);
    }
    public GraphLogSummary readMetadata(String relativePath) throws IOException {
        String safe = WorkflowConfigurationService.sanitizeRelativePath(relativePath);
        Path root = logRoot();
        Path file = root.resolve(safe).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root.toRealPath()))
            throw new IllegalArgumentException("Audit entry unavailable");
        JsonNode doc = mapper.readTree(file.toFile());
        if (doc.has("mergedGraph") || !doc.path("nodeCount").canConvertToInt() || !doc.path("edgeCount").canConvertToInt()
                || !Set.of("agent-call", "dynamic-graph").contains(doc.path("kind").asText()) || !doc.path("workflowId").isTextual())
            throw new IllegalArgumentException("Audit entry requires the fresh metadata contract");
        return new GraphLogSummary(safe, doc.path("workflowId").asText(), null, doc.path("kind").asText(),
                doc.path("workflowId").asText(), doc.path("timestamp").asText(), doc.path("plannerNodeId").asText(),
                doc.path("nodeCount").intValue(), doc.path("edgeCount").intValue());
    }
    public record GraphLogSummary(String fileName, String id, String label, String kind, String workflowId,
            String timestamp, String plannerNodeId, int nodeCount, int edgeCount) {}
}
