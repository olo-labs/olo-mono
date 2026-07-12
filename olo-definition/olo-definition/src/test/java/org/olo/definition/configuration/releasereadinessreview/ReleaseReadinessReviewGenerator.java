/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.releasereadinessreview;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

public final class ReleaseReadinessReviewGenerator extends ScenarioConfigurationGenerator {

    public ReleaseReadinessReviewGenerator() {
        super(
                ReleaseReadinessReviewDefinitions.ORCHESTRATOR_ID,
                ReleaseReadinessReviewDefinitions::orchestrator,
                entry(ReleaseReadinessReviewDefinitions.CHANGELOG_AGENT_ID, ReleaseReadinessReviewDefinitions::changelogAgent),
                entry(ReleaseReadinessReviewDefinitions.REGRESSION_AGENT_ID, ReleaseReadinessReviewDefinitions::regressionRiskAgent),
                entry(ReleaseReadinessReviewDefinitions.QA_AGENT_ID, ReleaseReadinessReviewDefinitions::qaSignoffAgent),
                entry(ReleaseReadinessReviewDefinitions.RELEASE_NOTES_AGENT_ID, ReleaseReadinessReviewDefinitions::releaseNotesAgent));
    }

    public static void main(String[] args) throws Exception {
        new ReleaseReadinessReviewGenerator().generateRoot(args, ReleaseReadinessReviewPaths::resolveConfigurationRoot);
    }
}
