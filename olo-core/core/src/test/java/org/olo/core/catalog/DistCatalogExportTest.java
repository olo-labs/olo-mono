package org.olo.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates {@code dist/catalog/} after {@code exportStudioCatalog} — includes workflow preset UI schema.
 */
class DistCatalogExportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void distCatalogIncludesConnectionRules() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode rules = catalog.path("defaults").path("connectionRules");
        assertThat(rules.path("strategy").asText()).isEqualTo("schema_match");
        assertThat(rules.path("primitives").toString()).contains("string");
    }

    @Test
    void distCatalogIncludesDesignerDefaults() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode designer = catalog.path("defaults").path("designer");
        assertThat(designer.path("nodeSize").path("width").asInt()).isEqualTo(200);
        assertThat(designer.path("nodeSize").path("height").asInt()).isEqualTo(80);
        assertThat(designer.path("resizable").asBoolean()).isTrue();
        assertThat(designer.path("draggable").asBoolean()).isTrue();
    }

    @Test
    void distCatalogIncludesParameterWidgetsInCatalogMetadata() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        assertThat(catalog.path("defaults").has("parameterWidgets")).isFalse();
        assertThat(catalog.path("catalogMetadata").path("parameterWidgets").isArray()).isTrue();
        assertThat(catalog.path("catalogMetadata").path("parameterWidgets").toString()).contains("TEXTAREA");
    }

    @Test
    void distCatalogIncludesConnectionPolicyDefaults() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode policy = catalog.path("defaults").path("connectionPolicy");
        assertThat(policy.path("maxInputs").asInt()).isEqualTo(-1);
        assertThat(policy.path("maxOutputs").asInt()).isEqualTo(-1);
        JsonNode switchNode = findNode(catalog.get("nodes"), "olo-core:SWITCH");
        assertThat(switchNode.get("connectionPolicy").get("maxInputs").asInt()).isEqualTo(1);
        assertThat(switchNode.get("connectionPolicy").get("maxOutputs").asInt()).isEqualTo(-1);
    }

    @Test
    void distCatalogIncludesHttpBodyVisibleWhen() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode http =
                catalog.get("tools").iterator().next();
        for (JsonNode tool : catalog.get("tools")) {
            if ("olo-core:http-tool".equals(tool.path("id").asText(null))) {
                http = tool;
                break;
            }
        }
        JsonNode body = null;
        for (JsonNode argument : http.get("parameters")) {
            if ("body".equals(argument.path("id").asText(null))) {
                body = argument;
                break;
            }
        }
        assertThat(body).isNotNull();
        assertThat(body.get("visibleWhen").get("method").asText()).isEqualTo("POST");
    }

    @Test
    void distCatalogIncludesNodeShapeMetadata() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode agent = findNode(catalog.get("nodes"), "olo-core:AGENT");
        assertThat(agent.get("designer").get("paletteGroup").asText()).isEqualTo("Agents");
        assertThat(agent.get("designer").get("nodeSize").get("width").asInt()).isEqualTo(300);
        assertThat(agent.get("designer").get("nodeSize").get("height").asInt()).isEqualTo(120);
        assertThat(agent.get("designer").has("resizable")).isFalse();
        JsonNode prompt = findNode(catalog.get("nodes"), "olo-core:PROMPT");
        assertThat(prompt.get("designer").has("nodeSize")).isFalse();
    }

    private static JsonNode findNode(JsonNode nodes, String id) {
        for (JsonNode node : nodes) {
            if (id.equals(node.path("id").asText(null))) {
                return node;
            }
        }
        return null;
    }

    @Test
    void distCatalogIncludesPortUiPosition() throws Exception {
        JsonNode catalog = MAPPER.readTree(resolveDistCatalogRoot().resolve("catalog.json").toFile());
        JsonNode agent = catalog.get("nodes").iterator().next();
        for (JsonNode node : catalog.get("nodes")) {
            if ("olo-core:AGENT".equals(node.path("id").asText(null))) {
                agent = node;
                break;
            }
        }
        assertThat(agent.get("inputs").get(0).get("ui").get("position").asText()).isEqualTo("LEFT");
        assertThat(agent.get("outputs").get(0).get("ui").get("position").asText()).isEqualTo("RIGHT");
    }

    @Test
    void distCatalogIncludesWorkflowPresetUiSchema() throws Exception {
        Path distCatalog = resolveDistCatalogRoot();
        assertThat(distCatalog.resolve("catalog.json")).exists();
        assertThat(distCatalog.resolve("workflow-presets.json")).exists();

        JsonNode catalog = MAPPER.readTree(distCatalog.resolve("catalog.json").toFile());
        assertThat(catalog.has("workflowPresets")).isTrue();
        JsonNode agent = findPreset(catalog.get("workflowPresets"), "agent");
        assertThat(agent).isNotNull();
        assertThat(agent.get("designer").get("paletteGroup").asText()).isEqualTo("Agents");
        JsonNode presetParameters = agent.get("parameters");
        assertThat(presetParameters.isArray()).isTrue();
        assertThat(findParameter(presetParameters, "temperature").get("id").asText()).isEqualTo("temperature");
        assertThat(findParameter(presetParameters, "temperature").get("label").asText()).isEqualTo("Temperature");
        assertThat(findParameter(presetParameters, "temperature").get("ui").get("widget").asText())
                .isEqualTo("SLIDER");
        assertThat(findParameter(presetParameters, "temperature").get("validation").get("minimum").asDouble())
                .isEqualTo(0.0);
        assertThat(findParameter(presetParameters, "model").get("required").asBoolean()).isTrue();
        assertThat(findParameter(presetParameters, "model").get("validation").get("minLength").asInt()).isEqualTo(1);

        JsonNode presets = MAPPER.readTree(distCatalog.resolve("workflow-presets.json").toFile());
        assertThat(presets.get("catalogType").asText()).isEqualTo("workflow-presets");
        assertThat(findPreset(presets.get("presets"), "agent")).isNotNull();
    }

    private static JsonNode findParameter(JsonNode parameters, String id) {
        for (JsonNode parameter : parameters) {
            if (id.equals(parameter.path("id").asText(null))) {
                return parameter;
            }
        }
        return null;
    }

    private static JsonNode findPreset(JsonNode presets, String id) {
        if (presets == null || !presets.isArray()) {
            return null;
        }
        for (JsonNode preset : presets) {
            if (id.equals(preset.path("id").asText(null))) {
                return preset;
            }
        }
        return null;
    }

    private static Path resolveDistCatalogRoot() {
        Path fromCore = Path.of("..", "dist", "catalog").normalize().toAbsolutePath();
        if (Files.isDirectory(fromCore)) {
            return fromCore;
        }
        Path fromRoot = Path.of("dist", "catalog").normalize().toAbsolutePath();
        return fromRoot;
    }
}
