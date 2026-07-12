/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.literaturereview;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

/** Writes the {@code literature-review} scenario collection. */
public final class LiteratureReviewGenerator extends ScenarioConfigurationGenerator {

    public LiteratureReviewGenerator() {
        super(
                LiteratureReviewDefinitions.ORCHESTRATOR_ID, LiteratureReviewDefinitions::orchestrator,
                entry(LiteratureReviewDefinitions.PAPER_DISCOVERY_AGENT_ID, LiteratureReviewDefinitions::paperDiscoveryAgent),
                entry(LiteratureReviewDefinitions.EVIDENCE_SYNTHESIS_AGENT_ID, LiteratureReviewDefinitions::evidenceSynthesisAgent),
                entry(LiteratureReviewDefinitions.GAP_ANALYSIS_AGENT_ID, LiteratureReviewDefinitions::gapAnalysisAgent),
                entry(LiteratureReviewDefinitions.RESEARCH_BRIEF_AGENT_ID, LiteratureReviewDefinitions::researchBriefAgent));
    }

    public static void main(String[] args) throws Exception {
        new LiteratureReviewGenerator().generateRoot(args, LiteratureReviewPaths::resolveConfigurationRoot);
    }
}
