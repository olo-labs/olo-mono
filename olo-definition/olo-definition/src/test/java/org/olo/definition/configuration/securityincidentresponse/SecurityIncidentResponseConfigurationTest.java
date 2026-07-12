/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.securityincidentresponse;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

class SecurityIncidentResponseConfigurationTest {

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path root = SecurityIncidentResponsePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(root, SecurityIncidentResponseDefinitions.ORCHESTRATOR_ID, SecurityIncidentResponseDefinitions.orchestrator());
        ScenarioConfigurationTestSupport.assertPreset(root, SecurityIncidentResponseDefinitions.THREAT_AGENT_ID, SecurityIncidentResponseDefinitions.threatDetectionAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, SecurityIncidentResponseDefinitions.FORENSICS_AGENT_ID, SecurityIncidentResponseDefinitions.forensicsAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, SecurityIncidentResponseDefinitions.CONTAINMENT_AGENT_ID, SecurityIncidentResponseDefinitions.containmentAgent());
        ScenarioConfigurationTestSupport.assertPreset(root, SecurityIncidentResponseDefinitions.SECURITY_REPORT_AGENT_ID, SecurityIncidentResponseDefinitions.securityReportAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndSecurityTools() throws IOException {
        Path root = SecurityIncidentResponsePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertOrchestratorChildAgents(
                root,
                SecurityIncidentResponseDefinitions.ORCHESTRATOR_ID,
                4,
                SecurityIncidentResponseDefinitions.THREAT_AGENT_ID,
                SecurityIncidentResponseDefinitions.FORENSICS_AGENT_ID,
                SecurityIncidentResponseDefinitions.CONTAINMENT_AGENT_ID,
                SecurityIncidentResponseDefinitions.SECURITY_REPORT_AGENT_ID);
    }
}
