/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionRuntimeRegistryLoaderTest {

    @Test
    void loadsRuntimeBindings() {
        ExtensionRuntimeRegistry registry =
                ExtensionRuntimeRegistryLoader.loadMerged(getClass().getClassLoader());

        assertThat(registry.schemaVersion()).isEqualTo("1.0");
        assertThat(registry.bindings()).hasSize(1);
        assertThat(registry.bindings().getFirst().id).isEqualTo("olo-core:http-tool");
        assertThat(registry.bindings().getFirst().implementationClass).isEqualTo("core.HttpTool");
        assertThat(registry.bindings().getFirst().spiInterface).isEqualTo("org.olo.spi.tool.Tool");
    }
}
