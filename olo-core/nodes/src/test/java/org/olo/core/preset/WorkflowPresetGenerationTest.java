package org.olo.core.preset;



import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.olo.annotation.OloCatalogLocations;



import java.io.InputStream;

import java.util.stream.StreamSupport;



import static org.assertj.core.api.Assertions.assertThat;



class WorkflowPresetGenerationTest {



    private static final ObjectMapper MAPPER = new ObjectMapper();



    @Test

    void compilesAuthoritativeWorkflowPresetCatalog() throws Exception {

        try (InputStream in =

                getClass().getClassLoader().getResourceAsStream(OloCatalogLocations.WORKFLOW_PRESETS_CATALOG)) {

            assertThat(in)

                    .as("annotation processor should emit %s", OloCatalogLocations.WORKFLOW_PRESETS_CATALOG)

                    .isNotNull();



            JsonNode root = MAPPER.readTree(in);

            assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");

            assertThat(root.get("moduleId").asText()).isEqualTo("olo-core-nodes");

            assertThat(root.get("catalogType").asText()).isEqualTo("workflow-presets");



            JsonNode agent = null;

            for (JsonNode preset : root.get("presets")) {

                if ("agent".equals(preset.get("id").asText())) {

                    agent = preset;

                    break;

                }

            }

            assertThat(agent).as("agent preset entry").isNotNull();

            JsonNode parameters = agent.get("parameters");

            assertThat(parameters.isArray()).isTrue();



            JsonNode systemPrompt = findParameter(parameters, "systemPrompt");
            assertThat(systemPrompt.get("required").asBoolean()).isFalse();

            JsonNode temperature = findParameter(parameters, "temperature");
            assertThat(temperature.get("required").asBoolean()).isFalse();
            assertThat(temperature.get("id").asText()).isEqualTo("temperature");
            assertThat(temperature.get("label").asText()).isEqualTo("Temperature");
            assertThat(temperature.get("type").asText()).isEqualTo("number");

            assertThat(temperature.get("defaultValue").asDouble()).isEqualTo(0.2);

            JsonNode validation = temperature.get("validation");

            assertThat(validation.get("minimum").asDouble()).isEqualTo(0);

            assertThat(validation.get("maximum").asDouble()).isEqualTo(2);

            assertThat(validation.get("step").asDouble()).isEqualTo(0.1);

            assertThat(temperature.get("ui").get("widget").asText()).isEqualTo("SLIDER");

            assertThat(temperature.get("ui").get("group").asText()).isEqualTo("Model Settings");



            JsonNode model = findParameter(parameters, "model");

            assertThat(model.get("required").asBoolean()).isFalse();
            assertThat(model.has("validation")).isFalse();

        }

    }



    private static JsonNode findParameter(JsonNode parameters, String id) {
        return StreamSupport.stream(parameters.spliterator(), false)
                .filter(node -> id.equals(node.path("id").asText(null)))
                .findFirst()
                .orElseThrow();
    }

}


