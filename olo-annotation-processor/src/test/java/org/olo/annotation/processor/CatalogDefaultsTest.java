package org.olo.annotation.processor;

import org.junit.jupiter.api.Test;
import org.olo.annotation.OloStability;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogDefaultsTest {

    @Test
    void materializesOwnershipAndStability() {
        assertThat(CatalogDefaults.materializeVersion("2.1.0")).isEqualTo("2.1.0");
        assertThat(CatalogDefaults.materializeVersion("")).isEqualTo("1.0.0");
        assertThat(CatalogDefaults.materializeProvider("acme-aws", "olo-core", "olo-core-tools"))
                .isEqualTo("acme-aws");
        assertThat(CatalogDefaults.materializeProvider("", "olo-core", "olo-core-tools"))
                .isEqualTo("olo-core");
        assertThat(CatalogDefaults.serializeStability(OloStability.STABLE, false)).isEqualTo("stable");
        assertThat(CatalogDefaults.serializeStability(OloStability.BETA, false)).isEqualTo("beta");
        assertThat(CatalogDefaults.serializeStability(OloStability.STABLE, true)).isEqualTo("experimental");
    }

    @Test
    void omitsDefaultPropertyGroup() {
        assertThat(CatalogDefaults.optionalPropertyGroup("")).isNull();
        assertThat(CatalogDefaults.optionalPropertyGroup("General")).isNull();
        assertThat(CatalogDefaults.optionalPropertyGroup("Advanced")).isEqualTo("Advanced");
    }

    @Test
    void omitsDefaultPropertyOrder() {
        assertThat(CatalogDefaults.materializePropertyOrder(Integer.MAX_VALUE)).isNull();
        assertThat(CatalogDefaults.materializePropertyOrder(0)).isZero();
        assertThat(CatalogDefaults.materializePropertyOrder(3)).isEqualTo(3);
    }

    @Test
    void parsesCapabilityJsonSchemas() {
        assertThat(CatalogDefaults.parseJsonSchema(""))
                .isNull();
        assertThat(CatalogDefaults.parseJsonSchema("{\"type\":\"object\"}").get("type").asText())
                .isEqualTo("object");
    }

    @Test
    void materializesRuntimeApiVersion() {
        assertThat(CatalogDefaults.materializeRuntimeContractVersion("2.1")).isEqualTo("2.1");
        assertThat(CatalogDefaults.materializeRuntimeContractVersion("")).isEqualTo("1.0");
    }

    @Test
    void materializesIsoDurations() {
        assertThat(CatalogDefaults.materializeIsoDuration("")).isNull();
        assertThat(CatalogDefaults.materializeIsoDuration("PT30S")).isEqualTo("PT30S");
    }

    @Test
    void materializesGlobalIds() {
        assertThat(CatalogDefaults.materializeGlobalId("http-tool", "olo-core")).isEqualTo("olo-core:http-tool");
        assertThat(CatalogDefaults.materializeGlobalId("olo-core:http-tool", "olo-core"))
                .isEqualTo("olo-core:http-tool");
        assertThat(CatalogDefaults.materializeGlobalId("PROMPT", "olo-core")).isEqualTo("olo-core:PROMPT");
    }

    @Test
    void humanizesCamelCaseIdentifiers() {
        assertThat(CatalogDefaults.humanizeIdentifier("maxIterations")).isEqualTo("Max Iterations");
        assertThat(CatalogDefaults.humanizeIdentifier("prompt")).isEqualTo("Prompt");
        assertThat(CatalogDefaults.humanizeIdentifier("systemPrompt")).isEqualTo("System Prompt");
    }
}
