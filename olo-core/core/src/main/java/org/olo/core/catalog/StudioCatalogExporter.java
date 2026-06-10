package org.olo.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.catalog.ExtensionCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes merged extension catalog JSON for editor consumers (olo-be / olo-ui).
 * <p>
 * Invoked by the {@code exportStudioCatalog} Gradle task into {@code dist/catalog/}:
 * <ul>
 *   <li>{@code catalog.json} — merged bundle served to Studio</li>
 *   <li>{@code nodes.json}, {@code tools.json}, {@code hooks.json} — per-type debug slices</li>
 * </ul>
 */
public final class StudioCatalogExporter {

    private StudioCatalogExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path outDir = args.length > 0 ? Path.of(args[0]) : Path.of("dist/catalog");
        Files.createDirectories(outDir);
        removeUnwantedFiles(outDir);

        ExtensionCatalog catalog = CoreExtensionCatalog.loadMerged();
        ObjectMapper mapper = CatalogJsonWriter.create();

        writePerTypeCatalog(outDir.resolve("nodes.json"), catalog, "nodes", mapper);
        writePerTypeCatalog(outDir.resolve("tools.json"), catalog, "tools", mapper);
        writePerTypeCatalog(outDir.resolve("hooks.json"), catalog, "hooks", mapper);
        writeMergedCatalog(outDir.resolve("catalog.json"), catalog, mapper);
    }

    private static void removeUnwantedFiles(Path outDir) throws IOException {
        if (!Files.isDirectory(outDir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(outDir)) {
            for (Path entry : entries.toList()) {
                String name = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && ("studio.json".equals(name) || "runtime.json".equals(name))) {
                    Files.delete(entry);
                }
            }
        }
    }

    private static void writePerTypeCatalog(
            Path file, ExtensionCatalog catalog, String catalogType, ObjectMapper mapper)
            throws IOException {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", catalog.schemaVersion());
        document.put("generatedBy", "olo-annotation-processor");
        document.put("generatedByVersion", "1.0.0");
        document.put("catalogType", catalogType);
        switch (catalogType) {
            case "nodes" -> document.put("nodes", catalog.nodes());
            case "tools" -> document.put("tools", catalog.tools());
            case "hooks" -> document.put("hooks", catalog.hooks());
            default -> throw new IllegalArgumentException("Unknown catalogType: " + catalogType);
        }
        mapper.writeValue(file.toFile(), document);
    }

    private static void writeMergedCatalog(Path file, ExtensionCatalog catalog, ObjectMapper mapper)
            throws IOException {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("schemaVersion", catalog.schemaVersion());
        bundle.put("generatedBy", "olo-annotation-processor");
        bundle.put("generatedByVersion", "1.0.0");
        bundle.put("nodes", catalog.nodes());
        bundle.put("tools", catalog.tools());
        bundle.put("hooks", catalog.hooks());
        mapper.writeValue(file.toFile(), bundle);
    }
}
