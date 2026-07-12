/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionRuntimeRegistry;
import org.olo.annotation.catalog.WorkflowPresetCatalogLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StudioCatalogExporterTest {

    @TempDir
    Path outDir;

    @Test
    void exportsGeneratedAtOnAllCatalogFiles() throws Exception {
        ExtensionCatalog catalog = CoreExtensionCatalog.loadMerged();
        ExtensionRuntimeRegistry runtime = CoreExtensionCatalog.loadRuntimeRegistry();
        ObjectMapper mapper = CatalogJsonWriter.create();
        String generatedAt = "2026-06-11T18:34:00Z";

        writeMergedCatalog(
                outDir.resolve("catalog.json"),
                catalog,
                WorkflowPresetCatalogLoader.loadMerged(CoreExtensionCatalog.class.getClassLoader()),
                generatedAt,
                mapper);
        writeRuntimeRegistry(outDir.resolve("runtime.json"), runtime, generatedAt, mapper);
        writePerTypeCatalog(outDir.resolve("nodes.json"), catalog, "nodes", generatedAt, mapper);

        JsonNode catalogRoot = new ObjectMapper().readTree(outDir.resolve("catalog.json").toFile());
        assertThat(catalogRoot.get("defaults").get("runtime").get("capabilities"))
                .extracting(JsonNode::asText)
                .containsExactly("DEBUG", "REPLAY");
        assertThat(catalogRoot.get("defaults").get("designer").get("nodeSize").get("width").asInt())
                .isEqualTo(200);
        assertThat(catalogRoot.get("defaults").has("parameterWidgets")).isFalse();
        assertThat(catalogRoot.get("catalogMetadata").get("parameterWidgets"))
                .extracting(JsonNode::asText)
                .contains("TEXTAREA", "SLIDER");

        assertHeader(outDir.resolve("catalog.json"), generatedAt);
        assertHeader(outDir.resolve("runtime.json"), generatedAt);
        assertHeader(outDir.resolve("nodes.json"), generatedAt);
    }

    private static void assertHeader(Path file, String generatedAt) throws Exception {
        JsonNode root = new ObjectMapper().readTree(file.toFile());
        assertThat(root.get("generatedBy").asText()).isEqualTo(StudioCatalogExporter.GENERATED_BY);
        assertThat(root.get("generatedByVersion").asText())
                .isEqualTo(StudioCatalogExporter.GENERATED_BY_VERSION);
        assertThat(root.get("generatedAt").asText()).isEqualTo(generatedAt);
    }

    private static void writePerTypeCatalog(
            Path file,
            ExtensionCatalog catalog,
            String catalogType,
            String generatedAt,
            ObjectMapper mapper)
            throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        StudioCatalogExporter.applyDocumentHeader(document, catalog.schemaVersion(), generatedAt);
        StudioCatalogExporter.applyStudioCatalogEnvelope(document);
        document.put("catalogType", catalogType);
        document.put("nodes", catalog.nodes());
        mapper.writeValue(file.toFile(), document);
    }

    private static void writeMergedCatalog(
            Path file,
            ExtensionCatalog catalog,
            java.util.List<org.olo.annotation.catalog.WorkflowPresetDescriptor> workflowPresets,
            String generatedAt,
            ObjectMapper mapper)
            throws Exception {
        Map<String, Object> bundle = new LinkedHashMap<>();
        StudioCatalogExporter.applyDocumentHeader(bundle, catalog.schemaVersion(), generatedAt);
        StudioCatalogExporter.applyStudioCatalogEnvelope(bundle);
        bundle.put("nodes", catalog.nodes());
        bundle.put("tools", catalog.tools());
        bundle.put("hooks", catalog.hooks());
        if (!workflowPresets.isEmpty()) {
            bundle.put("workflowPresets", workflowPresets);
        }
        mapper.writeValue(file.toFile(), bundle);
    }

    private static void writeRuntimeRegistry(
            Path file, ExtensionRuntimeRegistry runtime, String generatedAt, ObjectMapper mapper)
            throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        StudioCatalogExporter.applyDocumentHeader(document, runtime.schemaVersion(), generatedAt);
        document.put("bindings", runtime.bindings());
        mapper.writeValue(file.toFile(), document);
    }
}
