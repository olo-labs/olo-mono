package org.olo.annotation.processor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogDefaultsParameterDefaultTest {

    @Test
    void parsesNumericPropertyDefaultsFromAnnotationStrings() {
        assertThat(CatalogDefaults.parsePropertyDefault("number", "2")).isEqualTo(2);
        assertThat(CatalogDefaults.parsePropertyDefault("number", "1")).isEqualTo(1);
        assertThat(CatalogDefaults.parsePropertyDefault("number", "0.2")).isEqualTo(0.2);
        assertThat(CatalogDefaults.parsePropertyDefault("integer", "10")).isEqualTo(10);
    }

    @Test
    void leavesEnumDefaultsAsStrings() {
        assertThat(CatalogDefaults.parsePropertyDefault("enum", "GET")).isEqualTo("GET");
        assertThat(CatalogDefaults.parsePropertyDefault("string", "default")).isEqualTo("default");
    }
}
