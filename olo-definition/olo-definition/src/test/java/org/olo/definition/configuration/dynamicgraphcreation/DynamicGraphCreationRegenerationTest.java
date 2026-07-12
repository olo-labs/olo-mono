/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.dynamicgraphcreation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Regenerates {@code olo-configuration/dynamic-graph-creation/dynamic-graph-creation.json}. */
class DynamicGraphCreationRegenerationTest {

    @Test
    void regeneratesDynamicGraphCreationPreset() throws IOException {
        Path configurationRoot = DynamicGraphCreationPaths.resolveConfigurationRoot();
        new DynamicGraphCreationGenerator().generate(configurationRoot);

        assertThat(configurationRoot.resolve(DynamicGraphCreationDefinitions.FILE_NAME + ".json")).exists();
    }
}
