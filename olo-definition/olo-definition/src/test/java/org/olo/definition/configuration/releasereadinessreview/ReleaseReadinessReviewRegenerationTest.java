/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.releasereadinessreview;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseReadinessReviewRegenerationTest {

    @Test
    void regeneratesReleaseReadinessReviewCollection() throws IOException {
        Path configurationRoot = ReleaseReadinessReviewPaths.resolveConfigurationRoot();
        new ReleaseReadinessReviewGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(ReleaseReadinessReviewDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ReleaseReadinessReviewDefinitions.CHANGELOG_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ReleaseReadinessReviewDefinitions.REGRESSION_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ReleaseReadinessReviewDefinitions.QA_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ReleaseReadinessReviewDefinitions.RELEASE_NOTES_AGENT_ID + ".json")).exists();
    }
}
