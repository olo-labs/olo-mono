/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.apiintegrationtriage;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

/** Writes the {@code api-integration-triage} scenario collection. */
public final class ApiIntegrationTriageGenerator extends ScenarioConfigurationGenerator {

    public ApiIntegrationTriageGenerator() {
        super(
                ApiIntegrationTriageDefinitions.ORCHESTRATOR_ID, ApiIntegrationTriageDefinitions::orchestrator,
                entry(ApiIntegrationTriageDefinitions.ENDPOINT_PROBE_AGENT_ID, ApiIntegrationTriageDefinitions::endpointProbeAgent),
                entry(ApiIntegrationTriageDefinitions.DEPENDENCY_ANALYSIS_AGENT_ID, ApiIntegrationTriageDefinitions::dependencyAnalysisAgent),
                entry(ApiIntegrationTriageDefinitions.ERROR_CORRELATION_AGENT_ID, ApiIntegrationTriageDefinitions::errorCorrelationAgent),
                entry(ApiIntegrationTriageDefinitions.INTEGRATION_REPORT_AGENT_ID, ApiIntegrationTriageDefinitions::integrationReportAgent));
    }

    public static void main(String[] args) throws Exception {
        new ApiIntegrationTriageGenerator().generateRoot(args, ApiIntegrationTriagePaths::resolveConfigurationRoot);
    }
}
