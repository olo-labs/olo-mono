/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.config.OloConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Read-only access to dynamic subgraph injection logs under {@code olo-configuration/log/}.
 */
@Service
public class GraphLogService {

    private static final Logger log = LoggerFactory.getLogger(GraphLogService.class);

    private final OloConfigurationProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public GraphLogService(OloConfigurationProperties properties) {
        this.properties = properties;
    }

    public Path logRoot() {
        Path root = properties.resolvedLogDirectory();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Graph log directory not found: " + root);
        }
        return root;
    }

    public List<GraphLogSummary> listLogs() throws IOException {
        Path root = logRoot();
        List<GraphLogSummary> summaries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(
                            path -> root.relativize(path).toString().replace('\\', '/'),
                            Comparator.reverseOrder()))
                    .forEach(path -> {
                        try {
                            JsonNode document = mapper.readTree(path.toFile());
                            JsonNode mergedGraph = document.get("mergedGraph");
                            if (mergedGraph == null || !mergedGraph.isObject()) {
                                log.warn("Skipping log file without mergedGraph: {}", path);
                                return;
                            }
                            String relative = root.relativize(path).toString().replace('\\', '/');
                            String id = textOrNull(mergedGraph, "id");
                            if (id == null || id.isBlank()) {
                                id = textOrNull(document, "workflowId");
                            }
                            if (id == null || id.isBlank()) {
                                log.warn("Skipping log file without workflow id: {}", path);
                                return;
                            }
                            String label = textOrNull(mergedGraph, "label");
                            summaries.add(new GraphLogSummary(
                                    relative,
                                    id,
                                    label,
                                    textOrNull(document, "kind"),
                                    textOrNull(document, "workflowId"),
                                    textOrNull(document, "timestamp"),
                                    textOrNull(document, "plannerNodeId"),
                                    toolLabelsFromMergedGraph(mergedGraph)));
                        } catch (IOException e) {
                            log.warn("Skipping invalid log JSON {}: {}", path, e.getMessage());
                        }
                    });
        }
        return summaries;
    }

    public JsonNode readMergedGraph(String relativePath) throws IOException {
        Path file = resolveFile(relativePath);
        JsonNode document = mapper.readTree(file.toFile());
        JsonNode mergedGraph = document.get("mergedGraph");
        if (mergedGraph == null || !mergedGraph.isObject()) {
            throw new IllegalArgumentException("log file missing mergedGraph");
        }
        return mergedGraph;
    }

    private Path resolveFile(String relativePath) throws IOException {
        String safe = WorkflowConfigurationService.sanitizeRelativePath(relativePath);
        Path root = logRoot();
        Path resolved = root.resolve(safe).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid file path");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("log file not found");
        }
        return resolved;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static List<String> toolLabelsFromMergedGraph(JsonNode mergedGraph) {
        JsonNode nodes = mergedGraph.get("nodes");
        if (nodes == null || !nodes.isArray()) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (JsonNode node : nodes) {
            if (!"TOOL".equals(textOrNull(node, "type"))) {
                continue;
            }
            String nodeLabel = textOrNull(node, "label");
            if (nodeLabel != null && !nodeLabel.isBlank()) {
                labels.add(nodeLabel);
            }
        }
        return labels;
    }

    public record GraphLogSummary(
            String fileName,
            String id,
            String label,
            String kind,
            String workflowId,
            String timestamp,
            String plannerNodeId,
            List<String> toolLabels) {}
}
