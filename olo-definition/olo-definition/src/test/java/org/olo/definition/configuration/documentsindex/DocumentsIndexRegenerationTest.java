/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsindex;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentsIndexRegenerationTest {

    @Test
    void regeneratesDocumentsIndexPipeline() throws IOException {
        Path configurationRoot = DocumentsIndexPaths.resolveConfigurationRoot();
        new DocumentsIndexGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json")).exists();
        assertThat(configurationRoot.getParent()
                .resolve("current-active")
                .resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json"))
                .exists();
    }
}
