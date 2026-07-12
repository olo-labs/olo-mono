/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code META-INF/olo/catalog/workflow-types.json} generated from {@code @OloWorkflowType}.
 */
final class WorkflowTypesCatalogLoader {

    private static final String WORKFLOW_TYPES_CATALOG = "META-INF/olo/catalog/workflow-types.json";

    private WorkflowTypesCatalogLoader() {
    }

    static Map<String, Object> loadMerged(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : WorkflowTypesCatalogLoader.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(WORKFLOW_TYPES_CATALOG)) {
            if (in == null) {
                return Map.of();
            }
            ObjectMapper mapper = CatalogJsonWriter.create();
            JsonNode root = mapper.readTree(in);
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("schemaVersion", root.path("schemaVersion").asText("1.0"));
            document.put("workflowTypes", readArray(root.get("workflowTypes")));
            document.put("queues", readArray(root.get("queues")));
            return document;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load workflow type catalog", e);
        }
    }

    private static List<Object> readArray(JsonNode node) throws Exception {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        ObjectMapper mapper = CatalogJsonWriter.create();
        List<Object> values = new ArrayList<>();
        for (JsonNode entry : node) {
            values.add(mapper.convertValue(entry, Object.class));
        }
        return List.copyOf(values);
    }
}
