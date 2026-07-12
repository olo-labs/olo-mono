/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.NodeDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;
import org.olo.annotation.processor.CatalogDefaults;
import org.olo.annotation.processor.CatalogJsonMapper;
import org.olo.annotation.processor.model.ExtensionCatalogDocument;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes per-kind and merged extension catalog JSON resources to the annotation processor output.
 */
public final class CatalogDocumentWriter {

    private final ProcessingEnvironment processingEnv;

    public CatalogDocumentWriter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void write(
            String module,
            List<NodeDescriptor> nodes,
            List<ToolDescriptor> tools,
            List<HookDescriptor> hooks,
            List<RuntimeBindingDescriptor> runtimeBindings) {
        String generatedAt = Instant.now().toString();
        ObjectMapper mapper = CatalogJsonMapper.create();

        try {
            if (!nodes.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.NODES_CATALOG,
                        catalogDocument("nodes", module, generatedAt, Map.of("nodes", nodes)));
            }
            if (!tools.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.TOOLS_CATALOG,
                        catalogDocument("tools", module, generatedAt, Map.of("tools", tools)));
            }
            if (!hooks.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.HOOKS_CATALOG,
                        catalogDocument("hooks", module, generatedAt, Map.of("hooks", hooks)));
            }

            Map<String, Object> merged = new LinkedHashMap<>();
            CatalogDefaults.applyMergedHeader(merged, module, generatedAt);
            if (!nodes.isEmpty()) {
                merged.put("nodes", nodes);
            }
            if (!tools.isEmpty()) {
                merged.put("tools", tools);
            }
            if (!hooks.isEmpty()) {
                merged.put("hooks", hooks);
            }
            writeResource(mapper, OloCatalogLocations.MERGED_CATALOG, merged);

            if (!runtimeBindings.isEmpty()) {
                Map<String, Object> runtime = new LinkedHashMap<>();
                CatalogDefaults.applyMergedHeader(runtime, module, generatedAt);
                runtime.put("bindings", runtimeBindings);
                writeResource(mapper, OloCatalogLocations.RUNTIME_REGISTRY, runtime);
            }
        } catch (IOException e) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Failed to write extension catalog: " + e.getMessage());
        }
    }

    private static ExtensionCatalogDocument catalogDocument(
            String catalogType, String module, String generatedAt, Map<String, Object> body) {
        ExtensionCatalogDocument document = new ExtensionCatalogDocument();
        CatalogDefaults.applyDocumentHeader(document, module, catalogType, generatedAt);
        if (body.containsKey("nodes")) {
            document.nodes = (List<NodeDescriptor>) body.get("nodes");
        }
        if (body.containsKey("tools")) {
            document.tools = (List<ToolDescriptor>) body.get("tools");
        }
        if (body.containsKey("hooks")) {
            document.hooks = (List<HookDescriptor>) body.get("hooks");
        }
        return document;
    }

    private void writeResource(ObjectMapper mapper, String resourcePath, Object body) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (Writer writer = file.openWriter()) {
            mapper.writeValue(writer, body);
        }
    }
}
