/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.releasereadinessreview;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class ReleaseReadinessReviewConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = ReleaseReadinessReviewPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, ReleaseReadinessReviewDefinitions.ORCHESTRATOR_ID, ReleaseReadinessReviewDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, ReleaseReadinessReviewDefinitions.CHANGELOG_AGENT_ID, ReleaseReadinessReviewDefinitions.changelogAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ReleaseReadinessReviewDefinitions.REGRESSION_AGENT_ID, ReleaseReadinessReviewDefinitions.regressionRiskAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ReleaseReadinessReviewDefinitions.QA_AGENT_ID, ReleaseReadinessReviewDefinitions.qaSignoffAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ReleaseReadinessReviewDefinitions.RELEASE_NOTES_AGENT_ID, ReleaseReadinessReviewDefinitions.releaseNotesAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndReleaseTools() throws IOException {
        Path root = ReleaseReadinessReviewPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                ReleaseReadinessReviewDefinitions.ORCHESTRATOR_ID,
                4,
                ReleaseReadinessReviewDefinitions.CHANGELOG_AGENT_ID,
                ReleaseReadinessReviewDefinitions.REGRESSION_AGENT_ID,
                ReleaseReadinessReviewDefinitions.QA_AGENT_ID,
                ReleaseReadinessReviewDefinitions.RELEASE_NOTES_AGENT_ID);
    }
}
