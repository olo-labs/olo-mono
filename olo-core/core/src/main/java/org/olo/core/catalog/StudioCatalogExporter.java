package org.olo.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.catalog.ConnectionPolicyDescriptor;
import org.olo.annotation.catalog.DesignerDefaults;
import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.PortConnectionRules;
import org.olo.annotation.catalog.ExtensionRuntimeRegistry;
import org.olo.annotation.catalog.ExtensionRuntimeRegistryLoader;
import org.olo.annotation.catalog.WorkflowPresetCatalogLoader;
import org.olo.annotation.catalog.WorkflowPresetDescriptor;
import org.olo.spi.catalog.ParameterWidget;
import org.olo.spi.runtime.RuntimeCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes merged extension catalog JSON for editor consumers (olo-be / olo-ui).
 * <p>
 * Invoked by the {@code exportStudioCatalog} Gradle task into {@code dist/catalog/}:
 * <ul>
 *   <li>{@code catalog.json} — merged Studio bundle including {@code workflowPresets} UI schema</li>
 *   <li>{@code runtime.json} — merged JVM bindings keyed by global extension id</li>
 *   <li>{@code nodes.json}, {@code tools.json}, {@code hooks.json} — per-type debug slices</li>
 *   <li>{@code workflow-presets.json} — workflow parameter UI schema from {@code @OloWorkflowPreset}</li>
 * </ul>
 */
public final class StudioCatalogExporter {

    static final String GENERATED_BY = "olo-annotation-processor";
    static final String GENERATED_BY_VERSION = "1.0.0";

    private StudioCatalogExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path outDir = args.length > 0 ? Path.of(args[0]) : Path.of("dist/catalog");
        Files.createDirectories(outDir);
        removeUnwantedFiles(outDir);

        ExtensionCatalog catalog = CoreExtensionCatalog.loadMerged();
        ExtensionRuntimeRegistry runtime = CoreExtensionCatalog.loadRuntimeRegistry();
        ObjectMapper mapper = CatalogJsonWriter.create();
        String generatedAt = Instant.now().toString();

        writePerTypeCatalog(outDir.resolve("nodes.json"), catalog, "nodes", generatedAt, mapper);
        writePerTypeCatalog(outDir.resolve("tools.json"), catalog, "tools", generatedAt, mapper);
        writePerTypeCatalog(outDir.resolve("hooks.json"), catalog, "hooks", generatedAt, mapper);
        List<WorkflowPresetDescriptor> workflowPresets =
                WorkflowPresetCatalogLoader.loadMerged(CoreExtensionCatalog.class.getClassLoader());
        writeMergedCatalog(outDir.resolve("catalog.json"), catalog, workflowPresets, generatedAt, mapper);
        writeRuntimeRegistry(outDir.resolve("runtime.json"), runtime, generatedAt, mapper);
        writeWorkflowPresets(outDir.resolve("workflow-presets.json"), workflowPresets, generatedAt, mapper);
    }

    private static void removeUnwantedFiles(Path outDir) throws IOException {
        if (!Files.isDirectory(outDir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(outDir)) {
            for (Path entry : entries.toList()) {
                String name = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && "studio.json".equals(name)) {
                    Files.delete(entry);
                }
            }
        }
    }

    static void applyDocumentHeader(Map<String, Object> document, String schemaVersion, String generatedAt) {
        document.put("schemaVersion", schemaVersion);
        document.put("generatedBy", GENERATED_BY);
        document.put("generatedByVersion", GENERATED_BY_VERSION);
        document.put("generatedAt", generatedAt);
    }

    static void applyCatalogDefaults(Map<String, Object> document) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("runtime", Map.of("capabilities", RuntimeCapabilities.inheritedCatalogDefaultNames()));
        defaults.put("connectionRules", PortConnectionRules.catalogDefaults());
        defaults.put("connectionPolicy", ConnectionPolicyDescriptor.catalogDefaults());
        defaults.put("designer", DesignerDefaults.catalogDefaults());
        document.put("defaults", defaults);
    }

    /** Closed vocabularies and schema definitions for Studio — not per-extension inheritance defaults. */
    static void applyCatalogMetadata(Map<String, Object> document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("parameterWidgets", ParameterWidget.catalogValues());
        document.put("catalogMetadata", metadata);
    }

    static void applyStudioCatalogEnvelope(Map<String, Object> document) {
        applyCatalogDefaults(document);
        applyCatalogMetadata(document);
    }

    private static void writePerTypeCatalog(
            Path file,
            ExtensionCatalog catalog,
            String catalogType,
            String generatedAt,
            ObjectMapper mapper)
            throws IOException {
        Map<String, Object> document = new LinkedHashMap<>();
        applyDocumentHeader(document, catalog.schemaVersion(), generatedAt);
        if ("nodes".equals(catalogType) || "tools".equals(catalogType)) {
            applyStudioCatalogEnvelope(document);
        }
        document.put("catalogType", catalogType);
        switch (catalogType) {
            case "nodes" -> document.put("nodes", catalog.nodes());
            case "tools" -> document.put("tools", catalog.tools());
            case "hooks" -> document.put("hooks", catalog.hooks());
            default -> throw new IllegalArgumentException("Unknown catalogType: " + catalogType);
        }
        mapper.writeValue(file.toFile(), document);
    }

    private static void writeMergedCatalog(
            Path file,
            ExtensionCatalog catalog,
            List<WorkflowPresetDescriptor> workflowPresets,
            String generatedAt,
            ObjectMapper mapper)
            throws IOException {
        Map<String, Object> bundle = new LinkedHashMap<>();
        applyDocumentHeader(bundle, catalog.schemaVersion(), generatedAt);
        applyStudioCatalogEnvelope(bundle);
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
            throws IOException {
        Map<String, Object> document = new LinkedHashMap<>();
        applyDocumentHeader(document, runtime.schemaVersion(), generatedAt);
        document.put("bindings", runtime.bindings());
        mapper.writeValue(file.toFile(), document);
    }

    private static void writeWorkflowPresets(
            Path file, List<WorkflowPresetDescriptor> workflowPresets, String generatedAt, ObjectMapper mapper)
            throws IOException {
        if (workflowPresets.isEmpty()) {
            return;
        }
        Map<String, Object> document = new LinkedHashMap<>();
        applyDocumentHeader(document, "1.0", generatedAt);
        document.put("catalogType", "workflow-presets");
        document.put("moduleId", "olo-core");
        document.put("presets", workflowPresets);
        mapper.writeValue(file.toFile(), document);
    }
}
