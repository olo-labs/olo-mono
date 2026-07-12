/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.performancetriage;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

/** Writes the {@code performance-triage} scenario collection. */
public final class PerformanceTriageGenerator extends ScenarioConfigurationGenerator {

    public PerformanceTriageGenerator() {
        super(
                PerformanceTriageDefinitions.ORCHESTRATOR_ID, PerformanceTriageDefinitions::orchestrator,
                entry(PerformanceTriageDefinitions.LATENCY_AGENT_ID, PerformanceTriageDefinitions::latencyAnalysisAgent),
                entry(PerformanceTriageDefinitions.RESOURCE_AGENT_ID, PerformanceTriageDefinitions::resourcePressureAgent),
                entry(PerformanceTriageDefinitions.TUNING_AGENT_ID, PerformanceTriageDefinitions::optimizationAgent),
                entry(PerformanceTriageDefinitions.REPORT_AGENT_ID, PerformanceTriageDefinitions::performanceReportAgent));
    }

    public static void main(String[] args) throws Exception {
        new PerformanceTriageGenerator().generateRoot(args, PerformanceTriagePaths::resolveConfigurationRoot);
    }
}
