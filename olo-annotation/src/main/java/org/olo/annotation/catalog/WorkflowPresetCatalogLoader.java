package org.olo.annotation.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Loads generated {@link OloCatalogLocations#WORKFLOW_PRESETS_CATALOG} resources from the classpath.
 */
public final class WorkflowPresetCatalogLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowPresetCatalogLoader() {
    }

    public static List<WorkflowPresetDescriptor> loadMerged(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : WorkflowPresetCatalogLoader.class.getClassLoader();
        List<WorkflowPresetDescriptor> presets = new ArrayList<>();
        try (InputStream in = loader.getResourceAsStream(OloCatalogLocations.WORKFLOW_PRESETS_CATALOG)) {
            if (in == null) {
                return List.of();
            }
            JsonNode root = MAPPER.readTree(in);
            JsonNode presetNodes = root.get("presets");
            if (presetNodes == null || !presetNodes.isArray()) {
                return List.of();
            }
            for (JsonNode presetNode : presetNodes) {
                WorkflowPresetDescriptor preset = new WorkflowPresetDescriptor();
                preset.id = presetNode.path("id").asText(null);
                JsonNode designerNode = presetNode.get("designer");
                if (designerNode != null && designerNode.isObject()) {
                    preset.designer = MAPPER.treeToValue(designerNode, DesignerDescriptor.class);
                }
                loadParameters(presetNode.get("parameters"), preset.parameters);
                presets.add(preset);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load workflow preset catalog", e);
        }
        return List.copyOf(presets);
    }

    private static void loadParameters(JsonNode parameters, List<ParameterDescriptor> target) throws Exception {
        if (parameters == null || parameters.isNull()) {
            return;
        }
        if (parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                target.add(readParameter(parameter));
            }
            return;
        }
        if (parameters.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = parameters.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                ParameterDescriptor descriptor = readParameter(entry.getValue());
                if (descriptor.id == null || descriptor.id.isBlank()) {
                    descriptor.id = entry.getKey();
                }
                target.add(descriptor);
            }
        }
    }

    private static ParameterDescriptor readParameter(JsonNode node) throws Exception {
        ParameterDescriptor descriptor = MAPPER.treeToValue(node, ParameterDescriptor.class);
        if (descriptor.id == null || descriptor.id.isBlank()) {
            if (node.hasNonNull("label") && node.hasNonNull("name") && !node.has("id")) {
                descriptor.id = node.get("name").asText();
                descriptor.label = node.get("label").asText();
            } else if (descriptor.label != null && !node.has("id")) {
                descriptor.id = descriptor.label;
            }
        }
        return descriptor;
    }
}
