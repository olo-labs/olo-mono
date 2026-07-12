/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.literaturereview;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class LiteratureReviewConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = LiteratureReviewPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, LiteratureReviewDefinitions.ORCHESTRATOR_ID, LiteratureReviewDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, LiteratureReviewDefinitions.PAPER_DISCOVERY_AGENT_ID, LiteratureReviewDefinitions.paperDiscoveryAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, LiteratureReviewDefinitions.EVIDENCE_SYNTHESIS_AGENT_ID, LiteratureReviewDefinitions.evidenceSynthesisAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, LiteratureReviewDefinitions.GAP_ANALYSIS_AGENT_ID, LiteratureReviewDefinitions.gapAnalysisAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, LiteratureReviewDefinitions.RESEARCH_BRIEF_AGENT_ID, LiteratureReviewDefinitions.researchBriefAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndLiteratureTools() throws IOException {
        Path root = LiteratureReviewPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                LiteratureReviewDefinitions.ORCHESTRATOR_ID,
                4,
                LiteratureReviewDefinitions.PAPER_DISCOVERY_AGENT_ID,
                LiteratureReviewDefinitions.EVIDENCE_SYNTHESIS_AGENT_ID,
                LiteratureReviewDefinitions.GAP_ANALYSIS_AGENT_ID,
                LiteratureReviewDefinitions.RESEARCH_BRIEF_AGENT_ID);
    }
}
