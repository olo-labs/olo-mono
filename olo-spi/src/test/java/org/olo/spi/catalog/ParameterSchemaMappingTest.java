/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterSchemaMappingTest {

    @Test
    void mapsPropertyTypesToUnifiedSchema() {
        assertThat(ParameterSchemaMapping.fromPropertyType("TEXTAREA"))
                .isEqualTo(new ParameterSchemaMapping.MappedParameter("string", ParameterWidget.TEXTAREA));
        assertThat(ParameterSchemaMapping.fromPropertyType("ENUM"))
                .isEqualTo(new ParameterSchemaMapping.MappedParameter("enum", ParameterWidget.SELECT));
        assertThat(ParameterSchemaMapping.fromPropertyType("NUMBER"))
                .isEqualTo(new ParameterSchemaMapping.MappedParameter("number", ParameterWidget.NUMBER));
    }

    @Test
    void secretFlagOverridesType() {
        assertThat(ParameterSchemaMapping.fromPropertyType("STRING", true))
                .isEqualTo(new ParameterSchemaMapping.MappedParameter("string", ParameterWidget.SECRET));
    }
}
