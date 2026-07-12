/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.capacityplanning;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class CapacityPlanningConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = CapacityPlanningPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, CapacityPlanningDefinitions.ORCHESTRATOR_ID, CapacityPlanningDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, CapacityPlanningDefinitions.UTILIZATION_AGENT_ID, CapacityPlanningDefinitions.resourceUtilizationAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, CapacityPlanningDefinitions.COST_ESTIMATION_AGENT_ID, CapacityPlanningDefinitions.costEstimationAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, CapacityPlanningDefinitions.SCALING_AGENT_ID, CapacityPlanningDefinitions.scalingRecommendationAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, CapacityPlanningDefinitions.CAPACITY_REPORT_AGENT_ID, CapacityPlanningDefinitions.capacityReportAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndCapacityTools() throws IOException {
        Path root = CapacityPlanningPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                CapacityPlanningDefinitions.ORCHESTRATOR_ID,
                4,
                CapacityPlanningDefinitions.UTILIZATION_AGENT_ID,
                CapacityPlanningDefinitions.COST_ESTIMATION_AGENT_ID,
                CapacityPlanningDefinitions.SCALING_AGENT_ID,
                CapacityPlanningDefinitions.CAPACITY_REPORT_AGENT_ID);
    }
}
