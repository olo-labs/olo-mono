/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.literaturereview;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LiteratureReviewRegenerationTest {

    @Test
    void regeneratesLiteratureReviewCollection() throws IOException {
        Path configurationRoot = LiteratureReviewPaths.resolveConfigurationRoot();
        new LiteratureReviewGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(LiteratureReviewDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LiteratureReviewDefinitions.PAPER_DISCOVERY_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LiteratureReviewDefinitions.EVIDENCE_SYNTHESIS_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LiteratureReviewDefinitions.GAP_ANALYSIS_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LiteratureReviewDefinitions.RESEARCH_BRIEF_AGENT_ID + ".json")).exists();
    }
}
