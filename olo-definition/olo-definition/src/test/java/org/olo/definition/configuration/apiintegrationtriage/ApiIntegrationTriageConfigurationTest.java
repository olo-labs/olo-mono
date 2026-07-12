/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.apiintegrationtriage;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class ApiIntegrationTriageConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = ApiIntegrationTriagePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, ApiIntegrationTriageDefinitions.ORCHESTRATOR_ID, ApiIntegrationTriageDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, ApiIntegrationTriageDefinitions.ENDPOINT_PROBE_AGENT_ID, ApiIntegrationTriageDefinitions.endpointProbeAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ApiIntegrationTriageDefinitions.DEPENDENCY_ANALYSIS_AGENT_ID, ApiIntegrationTriageDefinitions.dependencyAnalysisAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ApiIntegrationTriageDefinitions.ERROR_CORRELATION_AGENT_ID, ApiIntegrationTriageDefinitions.errorCorrelationAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, ApiIntegrationTriageDefinitions.INTEGRATION_REPORT_AGENT_ID, ApiIntegrationTriageDefinitions.integrationReportAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndIntegrationTools() throws IOException {
        Path root = ApiIntegrationTriagePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                ApiIntegrationTriageDefinitions.ORCHESTRATOR_ID,
                4,
                ApiIntegrationTriageDefinitions.ENDPOINT_PROBE_AGENT_ID,
                ApiIntegrationTriageDefinitions.DEPENDENCY_ANALYSIS_AGENT_ID,
                ApiIntegrationTriageDefinitions.ERROR_CORRELATION_AGENT_ID,
                ApiIntegrationTriageDefinitions.INTEGRATION_REPORT_AGENT_ID);
    }
}
