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
    void humanizesCamelCaseIdentifiers() {
        assertThat(CatalogDefaults.humanizeIdentifier("maxIterations")).isEqualTo("Max Iterations");
        assertThat(CatalogDefaults.humanizeIdentifier("prompt")).isEqualTo("Prompt");
        assertThat(CatalogDefaults.humanizeIdentifier("systemPrompt")).isEqualTo("System Prompt");
    }
}
