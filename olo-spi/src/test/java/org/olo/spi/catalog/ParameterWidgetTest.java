package org.olo.spi.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterWidgetTest {

    @Test
    void parsesCanonicalEnumNames() {
        assertThat(ParameterWidget.parse("SLIDER")).isEqualTo(ParameterWidget.SLIDER);
        assertThat(ParameterWidget.parse("MODEL_SELECTOR")).isEqualTo(ParameterWidget.MODEL_SELECTOR);
    }

    @Test
    void normalizesLegacyWireNames() {
        assertThat(ParameterWidget.parse("slider")).isEqualTo(ParameterWidget.SLIDER);
        assertThat(ParameterWidget.parse("model_selector")).isEqualTo(ParameterWidget.MODEL_SELECTOR);
        assertThat(ParameterWidget.parse("switch")).isEqualTo(ParameterWidget.BOOLEAN);
        assertThat(ParameterWidget.normalizeCatalogValue("textarea")).isEqualTo("TEXTAREA");
    }

    @Test
    void exposesFullCatalogList() {
        assertThat(ParameterWidget.catalogValues())
                .containsExactly(
                        "STRING",
                        "TEXTAREA",
                        "NUMBER",
                        "SLIDER",
                        "BOOLEAN",
                        "SELECT",
                        "MULTI_SELECT",
                        "JSON",
                        "CODE",
                        "MODEL_SELECTOR",
                        "SECRET",
                        "CRON");
    }
}
