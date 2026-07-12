/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.port;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortSchemaCompatibilityTest {

    @Test
    void acceptsMatchingPrimitivesRegardlessOfCase() {
        assertThat(PortSchemaCompatibility.compatible("string", "string")).isTrue();
        assertThat(PortSchemaCompatibility.compatible("String", "string")).isTrue();
        assertThat(PortSchemaCompatibility.compatible("str", "string")).isTrue();
    }

    @Test
    void rejectsPrimitiveMismatchesWithoutCoercion() {
        assertThat(PortSchemaCompatibility.compatible("string", "number")).isFalse();
        assertThat(PortSchemaCompatibility.compatible("number", "string")).isFalse();
        assertThat(PortSchemaCompatibility.compatible("integer", "number")).isFalse();
    }

    @Test
    void acceptsDomainTypeExactMatch() {
        assertThat(PortSchemaCompatibility.compatible("Stock[]", "Stock[]")).isTrue();
        assertThat(PortSchemaCompatibility.compatible("Stock[]", "stock[]")).isFalse();
    }

    @Test
    void acceptsWildcardTargetsAndSources() {
        assertThat(PortSchemaCompatibility.compatible("string", "any")).isTrue();
        assertThat(PortSchemaCompatibility.compatible("string", "*")).isTrue();
        assertThat(PortSchemaCompatibility.compatible("any", "number")).isTrue();
    }

    @Test
    void rejectsArrayElementMismatch() {
        assertThat(PortSchemaCompatibility.compatible("string[]", "number[]")).isFalse();
        assertThat(PortSchemaCompatibility.compatible("string[]", "string[]")).isTrue();
    }

    @Test
    void rejectsScalarToArrayMismatch() {
        assertThat(PortSchemaCompatibility.compatible("string", "string[]")).isFalse();
    }
}
