/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionCatalogLoader;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves plugin-driven UI form schema for human-in-the-loop steps from the extension catalog.
 */
public final class HumanInputSchemaResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HumanInputSchemaResolver() {
    }

    public static Map<String, Object> resolveFormSchema(String inputPluginId) {
        if (inputPluginId == null || inputPluginId.isBlank()) {
            return Map.of();
        }
        ToolDescriptor tool = findTool(inputPluginId.trim());
        if (tool == null) {
            return Map.of(
                    "inputPluginId", inputPluginId,
                    "inputType", "plugin",
                    "parameters", List.of(),
                    "options", HumanInputPluginOptions.optionsFor(inputPluginId));
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("inputPluginId", tool.id != null ? tool.id : inputPluginId);
        schema.put("inputType", "plugin");
        schema.put("pluginName", tool.name);
        schema.put("pluginDescription", tool.description);
        schema.put("parameters", serializeParameters(tool.parameters));
        schema.put("options", HumanInputPluginOptions.optionsFor(inputPluginId));
        if (tool.contract != null) {
            schema.put("contract", MAPPER.convertValue(tool.contract, Map.class));
        }
        return schema;
    }

    private static List<Map<String, Object>> serializeParameters(List<ParameterDescriptor> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> serialized = new ArrayList<>(parameters.size());
        for (ParameterDescriptor parameter : parameters) {
            serialized.add(MAPPER.convertValue(parameter, Map.class));
        }
        return serialized;
    }

    public static void enrichWaitingOutput(Map<String, Object> output, String inputPluginId) {
        Objects.requireNonNull(output, "output");
        if (inputPluginId == null || inputPluginId.isBlank()) {
            return;
        }
        output.putAll(resolveFormSchema(inputPluginId));
        if (output.get("prompt") == null && output.get("title") != null) {
            output.put("prompt", output.get("title"));
        }
        if (output.get("message") == null && output.get("description") != null) {
            output.put("message", output.get("description"));
        }
    }

    private static ToolDescriptor findTool(String inputPluginId) {
        ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged();
        for (ToolDescriptor tool : catalog.tools()) {
            if (inputPluginId.equals(tool.id)) {
                return tool;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> copyAsMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            return MAPPER.convertValue(map, Map.class);
        }
        return MAPPER.convertValue(value, Map.class);
    }
}
