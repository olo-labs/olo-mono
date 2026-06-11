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

/**
 * Loads and merges {@link OloCatalogLocations#RUNTIME_REGISTRY} from the classpath.
 * <p>
 * Duplicate extension ids across JARs: first occurrence wins; a WARNING is logged.
 */
public final class ExtensionRuntimeRegistryLoader {

    private static final System.Logger LOG =
            System.getLogger(ExtensionRuntimeRegistryLoader.class.getName());

    private static final String DEFAULT_SCHEMA_VERSION = "1.0";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExtensionRuntimeRegistryLoader() {
    }

    public static ExtensionRuntimeRegistry loadMerged() {
        return loadMerged(Thread.currentThread().getContextClassLoader());
    }

    public static ExtensionRuntimeRegistry loadMerged(ClassLoader classLoader) {
        ClassLoader loader =
                classLoader != null ? classLoader : ExtensionRuntimeRegistryLoader.class.getClassLoader();
        Map<String, RuntimeBindingDescriptor> byId = new LinkedHashMap<>();
        Map<String, String> moduleById = new LinkedHashMap<>();
        String schemaVersion = DEFAULT_SCHEMA_VERSION;

        for (RegistryFile file : readAllResources(loader, OloCatalogLocations.RUNTIME_REGISTRY)) {
            if (file.schemaVersion != null && !file.schemaVersion.isBlank()) {
                schemaVersion = file.schemaVersion;
            }
            String module = file.moduleId != null && !file.moduleId.isBlank() ? file.moduleId : "unknown";
            mergeBindings(file.bindings, module, byId, moduleById);
        }

        return new ExtensionRuntimeRegistry(schemaVersion, new ArrayList<>(byId.values()));
    }

    private static void mergeBindings(
            List<RuntimeBindingDescriptor> bindings,
            String module,
            Map<String, RuntimeBindingDescriptor> byId,
            Map<String, String> moduleById) {
        if (bindings == null) {
            return;
        }
        for (RuntimeBindingDescriptor binding : bindings) {
            if (binding == null) {
                continue;
            }
            String id = binding.id();
            if (id == null || id.isBlank()) {
                continue;
            }
            if (byId.containsKey(id)) {
                LOG.log(
                        System.Logger.Level.WARNING,
                        "Duplicate runtime binding id detected: {0}{1}Winner:{1}  {2}{1}Ignored:{1}  {3}",
                        id,
                        System.lineSeparator(),
                        moduleById.get(id),
                        module);
                continue;
            }
            byId.put(id, binding);
            moduleById.put(id, module);
        }
    }

    private static List<RegistryFile> readAllResources(ClassLoader loader, String path) {
        try {
            Enumeration<URL> urls = loader.getResources(path);
            List<RegistryFile> files = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream in = url.openStream()) {
                    files.add(MAPPER.readValue(in, RegistryFile.class));
                }
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read runtime registry: " + path, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RegistryFile {
        @JsonAlias("version")
        public String schemaVersion;
        @JsonAlias("module")
        public String moduleId;
        public List<RuntimeBindingDescriptor> bindings;
    }
}
