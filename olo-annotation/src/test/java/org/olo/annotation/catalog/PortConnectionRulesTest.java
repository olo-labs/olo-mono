/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortConnectionRulesTest {

    @Test
    void rejectsStringToNumber() {
        assertThat(PortConnectionRules.compatible("string", "number")).isFalse();
        assertThat(PortConnectionRules.compatible("string", "string")).isTrue();
    }

    @Test
    void exposesCatalogDefaults() {
        assertThat(PortConnectionRules.catalogDefaults())
                .containsEntry("strategy", "schema_match")
                .containsKey("wildcards")
                .containsKey("primitives");
    }
}
