/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.capacityplanning;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

/** Writes the {@code capacity-planning} scenario collection. */
public final class CapacityPlanningGenerator extends ScenarioConfigurationGenerator {

    public CapacityPlanningGenerator() {
        super(
                CapacityPlanningDefinitions.ORCHESTRATOR_ID, CapacityPlanningDefinitions::orchestrator,
                entry(CapacityPlanningDefinitions.UTILIZATION_AGENT_ID, CapacityPlanningDefinitions::resourceUtilizationAgent),
                entry(CapacityPlanningDefinitions.COST_ESTIMATION_AGENT_ID, CapacityPlanningDefinitions::costEstimationAgent),
                entry(CapacityPlanningDefinitions.SCALING_AGENT_ID, CapacityPlanningDefinitions::scalingRecommendationAgent),
                entry(CapacityPlanningDefinitions.CAPACITY_REPORT_AGENT_ID, CapacityPlanningDefinitions::capacityReportAgent));
    }

    public static void main(String[] args) throws Exception {
        new CapacityPlanningGenerator().generateRoot(args, CapacityPlanningPaths::resolveConfigurationRoot);
    }
}
