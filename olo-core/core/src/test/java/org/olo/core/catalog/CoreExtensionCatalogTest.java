/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreExtensionCatalogTest {

    @Test
    void loadsMergedCoreExtensions() {
        var catalog = CoreExtensionCatalog.loadMerged();
        assertThat(catalog.nodes()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(catalog.hooks()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(catalog.nodes().stream().map(n -> n.id).toList())
                .contains("olo-core:PROMPT", "olo-core:AGENT");
        assertThat(catalog.schemaVersion()).isEqualTo("1.0");
        catalog.nodes().forEach(n -> {
            assertThat(n.emoji).as("node %s emoji", n.id).isNotBlank();
            assertThat(n.version).as("node %s version", n.id).isEqualTo("1.0.0");
            assertThat(n.provider).as("node %s provider", n.id).isEqualTo("olo-core");
            assertThat(n.stability).as("node %s stability", n.id).isNotBlank();
        });
        catalog.hooks().forEach(h -> {
            assertThat(h.emoji).as("hook %s emoji", h.id).isNotBlank();
            assertThat(h.version).as("hook %s version", h.id).isEqualTo("1.0.0");
            assertThat(h.provider).as("hook %s provider", h.id).isEqualTo("olo-core");
            assertThat(h.stability).as("hook %s stability", h.id).isNotBlank();
        });
        assertThat(catalog.nodes().stream()
                        .filter(n -> "olo-core:AGENT".equals(n.id))
                        .findFirst()
                        .orElseThrow()
                        .stability)
                .isEqualTo("experimental");
        var runtime = CoreExtensionCatalog.loadRuntimeRegistry();
        assertThat(runtime.bindings()).hasSizeGreaterThanOrEqualTo(9);
        assertThat(runtime.bindings().stream().map(b -> b.id).toList())
                .contains("olo-core:PROMPT")
                .doesNotContain("olo-core:http-tool");
    }
}
