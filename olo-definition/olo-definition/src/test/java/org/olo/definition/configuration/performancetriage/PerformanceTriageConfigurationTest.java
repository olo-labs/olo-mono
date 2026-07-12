/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.performancetriage;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class PerformanceTriageConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = PerformanceTriagePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, PerformanceTriageDefinitions.ORCHESTRATOR_ID, PerformanceTriageDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, PerformanceTriageDefinitions.LATENCY_AGENT_ID, PerformanceTriageDefinitions.latencyAnalysisAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, PerformanceTriageDefinitions.RESOURCE_AGENT_ID, PerformanceTriageDefinitions.resourcePressureAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, PerformanceTriageDefinitions.TUNING_AGENT_ID, PerformanceTriageDefinitions.optimizationAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, PerformanceTriageDefinitions.REPORT_AGENT_ID, PerformanceTriageDefinitions.performanceReportAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndMetricTools() throws IOException {
        Path root = PerformanceTriagePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                PerformanceTriageDefinitions.ORCHESTRATOR_ID,
                4,
                PerformanceTriageDefinitions.LATENCY_AGENT_ID,
                PerformanceTriageDefinitions.RESOURCE_AGENT_ID,
                PerformanceTriageDefinitions.TUNING_AGENT_ID,
                PerformanceTriageDefinitions.REPORT_AGENT_ID);
    }
}
