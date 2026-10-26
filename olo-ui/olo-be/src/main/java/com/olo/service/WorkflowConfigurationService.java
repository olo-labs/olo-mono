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
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class WorkflowConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowConfigurationService.class);

    private final OloConfigurationProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public WorkflowConfigurationService(OloConfigurationProperties properties) {
        this.properties = properties;
    }

    public Path configurationRoot() {
        Path root = properties.resolvedDirectory();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Configuration directory not found: " + root);
        }
        return root;
    }

    public List<WorkflowSummary> listWorkflows() throws IOException {
        Path root = configurationRoot();
        List<WorkflowSummary> summaries = new ArrayList<>();
        // Recursive: supports current-active/agents/agent.json and similar layouts.
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> isWorkflowPresetFile(root, path))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString().replace('\\', '/')))
                    .forEach(path -> {
                        try {
                            JsonNode document = mapper.readTree(path.toFile());
                            String id = textOrNull(document, "id");
                            if (id == null || id.isBlank()) {
                                log.warn("Skipping workflow file without id: {}", path);
                                return;
                            }
                            String label = textOrNull(document, "label");
                            String queue = textOrNull(document, "queue");
                            String workflowType = textOrNull(document, "workflowType");
                            String relative = root.relativize(path).toString().replace('\\', '/');
                            summaries.add(new WorkflowSummary(relative, id, label, queue, workflowType));
                        } catch (IOException e) {
                            log.warn("Skipping invalid workflow JSON {}: {}", path, e.getMessage());
                        }
                    });
        }
        return summaries;
    }

    public JsonNode readWorkflow(String relativePath) throws IOException {
        Path file = resolveFile(relativePath);
        return mapper.readTree(file.toFile());
    }

    public WorkflowSummary writeWorkflow(String relativePath, JsonNode document) throws IOException {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("workflow document must be a JSON object");
        }
        String id = textOrNull(document, "id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("workflow id is required");
        }
        String targetPath = sanitizeRelativePath(
                relativePath != null && !relativePath.isBlank() ? relativePath : id + ".json");
        Path file = resolveFile(targetPath);
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), document);
        return new WorkflowSummary(targetPath, id, textOrNull(document, "label"),
                textOrNull(document, "queue"), textOrNull(document, "workflowType"));
    }

    public void deleteWorkflow(String relativePath) throws IOException {
        Files.deleteIfExists(resolveFile(relativePath));
    }

    static boolean isWorkflowPresetFile(Path root, Path file) {
        String name = file.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return false;
        }
        if (name.toLowerCase(Locale.ROOT).endsWith(".injection.json")) {
            return false;
        }
        String relative = root.relativize(file).toString().replace('\\', '/');
        return !relative.startsWith("log/") && !relative.contains("/log/");
    }

    private Path resolveFile(String relativePath) throws IOException {
        String safe = sanitizeRelativePath(relativePath);
        Path root = configurationRoot();
        Path resolved = root.resolve(safe).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid file path");
        }
        return resolved;
    }

    static String sanitizeRelativePath(String relativePath) {
        String trimmed = relativePath.trim().replace('\\', '/');
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty() || trimmed.contains("..")) {
            throw new IllegalArgumentException("invalid workflow file path");
        }
        String[] segments = trimmed.split("/");
        StringBuilder safe = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if (!segment.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("invalid workflow file path");
            }
            if (!safe.isEmpty()) {
                safe.append('/');
            }
            safe.append(segment);
        }
        if (safe.isEmpty()) {
            throw new IllegalArgumentException("invalid workflow file path");
        }
        if (!safe.toString().endsWith(".json")) {
            safe.append(".json");
        }
        return safe.toString();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    public record WorkflowSummary(
            String fileName,
            String id,
            String label,
            String queue,
            String workflowType) {}
}
