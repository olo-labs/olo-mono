package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads and merges extension catalog JSON resources from the classpath for UI and tooling.
 * <p>
 * Reads only authoritative per-type files ({@code nodes.json}, {@code tools.json}, {@code hooks.json}).
 * Module-level {@code catalog.json} is generated for convenience (indexing, inspection) and is not merged here.
 * <p>
 * Duplicate extension ids across JARs: first occurrence wins; a WARNING is logged naming winner and ignored module.
 */
public final class ExtensionCatalogLoader {

    private static final System.Logger LOG = System.getLogger(ExtensionCatalogLoader.class.getName());

    private static final String DEFAULT_SCHEMA_VERSION = "1.0";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExtensionCatalogLoader() {
    }

    public static ExtensionCatalog loadMerged() {
        return loadMerged(Thread.currentThread().getContextClassLoader());
    }

    public static ExtensionCatalog loadMerged(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : ExtensionCatalogLoader.class.getClassLoader();
        MergeState<NodeDescriptor> nodes = new MergeState<>("node", NodeDescriptor::id);
        MergeState<ToolDescriptor> tools = new MergeState<>("tool", ToolDescriptor::id);
        MergeState<HookDescriptor> hooks = new MergeState<>("hook", HookDescriptor::id);
        String schemaVersion = DEFAULT_SCHEMA_VERSION;

        for (String resource : OloCatalogLocations.AUTHORITATIVE_CATALOGS) {
            for (CatalogFile file : readAllResources(loader, resource)) {
                if (file.schemaVersion != null && !file.schemaVersion.isBlank()) {
                    schemaVersion = file.schemaVersion;
                }
                String module = file.moduleId != null && !file.moduleId.isBlank() ? file.moduleId : resource;
                nodes.merge(file.nodes, module);
                tools.merge(file.tools, module);
                hooks.merge(file.hooks, module);
            }
        }

        return new ExtensionCatalog(schemaVersion, nodes.values(), tools.values(), hooks.values());
    }

    private static List<CatalogFile> readAllResources(ClassLoader loader, String path) {
        if (OloCatalogLocations.MERGED_CATALOG.equals(path)) {
            throw new IllegalStateException(
                    "catalog.json is a per-module convenience bundle and must not be loaded by ExtensionCatalogLoader");
        }
        if (!OloCatalogLocations.AUTHORITATIVE_CATALOGS.contains(path)) {
            throw new IllegalStateException("Unexpected catalog resource: " + path);
        }
        try {
            Enumeration<URL> urls = loader.getResources(path);
            List<CatalogFile> files = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream in = url.openStream()) {
                    files.add(MAPPER.readValue(in, CatalogFile.class));
                }
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read catalog resources: " + path, e);
        }
    }

    private static final class MergeState<T> {
        private final String kindLabel;
        private final Function<T, String> idExtractor;
        private final Map<String, T> byId = new LinkedHashMap<>();
        private final Map<String, String> moduleById = new LinkedHashMap<>();

        MergeState(String kindLabel, Function<T, String> idExtractor) {
            this.kindLabel = kindLabel;
            this.idExtractor = idExtractor;
        }

        void merge(List<T> items, String module) {
            if (items == null) {
                return;
            }
            for (T item : items) {
                if (item == null) {
                    continue;
                }
                String id = idExtractor.apply(item);
                if (id == null || id.isBlank()) {
                    continue;
                }
                if (byId.containsKey(id)) {
                    LOG.log(
                            System.Logger.Level.WARNING,
                            "Duplicate {0} id detected: {1}{2}Winner:{2}  {3}{2}Ignored:{2}  {4}",
                            kindLabel,
                            id,
                            System.lineSeparator(),
                            moduleById.get(id),
                            module);
                    continue;
                }
                byId.put(id, item);
                moduleById.put(id, module);
            }
        }

        List<T> values() {
            return new ArrayList<>(byId.values());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CatalogFile {
        @JsonAlias("version")
        public String schemaVersion;
        @JsonAlias("module")
        public String moduleId;
        public List<NodeDescriptor> nodes;
        public List<ToolDescriptor> tools;
        public List<HookDescriptor> hooks;
    }
}
