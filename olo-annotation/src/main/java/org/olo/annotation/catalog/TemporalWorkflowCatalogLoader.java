/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads generated {@link OloCatalogLocations#WORKFLOW_TYPES_CATALOG} resources from the classpath.
 */
public final class TemporalWorkflowCatalogLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TemporalWorkflowCatalogLoader() {
    }

    public static TemporalWorkflowCatalog loadMerged(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : TemporalWorkflowCatalogLoader.class.getClassLoader();
        Map<String, TemporalWorkflowTypeDescriptor> workflowTypes = new LinkedHashMap<>();
        Map<String, TemporalQueueDescriptor> queues = new LinkedHashMap<>();

        try (InputStream in = loader.getResourceAsStream(OloCatalogLocations.WORKFLOW_TYPES_CATALOG)) {
            if (in == null) {
                return emptyCatalog();
            }
            JsonNode root = MAPPER.readTree(in);
            mergeDocument(root, workflowTypes, queues);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load workflow type catalog", e);
        }

        TemporalWorkflowCatalog catalog = new TemporalWorkflowCatalog();
        catalog.workflowTypes = workflowTypes.values().stream()
                .sorted(Comparator.comparing(type -> type.id))
                .toList();
        catalog.queues = queues.values().stream()
                .sorted(Comparator.comparing(queue -> queue.name))
                .toList();
        return catalog;
    }

    private static void mergeDocument(
            JsonNode root,
            Map<String, TemporalWorkflowTypeDescriptor> workflowTypes,
            Map<String, TemporalQueueDescriptor> queues)
            throws Exception {
        JsonNode typeNodes = root.get("workflowTypes");
        if (typeNodes != null && typeNodes.isArray()) {
            for (JsonNode typeNode : typeNodes) {
                TemporalWorkflowTypeDescriptor descriptor =
                        MAPPER.treeToValue(typeNode, TemporalWorkflowTypeDescriptor.class);
                if (descriptor.id != null && !descriptor.id.isBlank()) {
                    workflowTypes.put(descriptor.id, descriptor);
                }
            }
        }
        JsonNode queueNodes = root.get("queues");
        if (queueNodes != null && queueNodes.isArray()) {
            for (JsonNode queueNode : queueNodes) {
                TemporalQueueDescriptor descriptor = MAPPER.treeToValue(queueNode, TemporalQueueDescriptor.class);
                if (descriptor.name != null && !descriptor.name.isBlank()) {
                    queues.put(descriptor.name, descriptor);
                }
            }
        }
    }

    private static TemporalWorkflowCatalog emptyCatalog() {
        TemporalWorkflowCatalog catalog = new TemporalWorkflowCatalog();
        catalog.workflowTypes = List.of();
        catalog.queues = List.of();
        return catalog;
    }
}
