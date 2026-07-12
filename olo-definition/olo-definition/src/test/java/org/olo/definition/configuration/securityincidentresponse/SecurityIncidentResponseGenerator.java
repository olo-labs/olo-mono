/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.securityincidentresponse;

import org.olo.definition.configuration.scenario.ScenarioConfigurationGenerator;

public final class SecurityIncidentResponseGenerator extends ScenarioConfigurationGenerator {

    public SecurityIncidentResponseGenerator() {
        super(
                SecurityIncidentResponseDefinitions.ORCHESTRATOR_ID,
                SecurityIncidentResponseDefinitions::orchestrator,
                entry(SecurityIncidentResponseDefinitions.THREAT_AGENT_ID, SecurityIncidentResponseDefinitions::threatDetectionAgent),
                entry(SecurityIncidentResponseDefinitions.FORENSICS_AGENT_ID, SecurityIncidentResponseDefinitions::forensicsAgent),
                entry(SecurityIncidentResponseDefinitions.CONTAINMENT_AGENT_ID, SecurityIncidentResponseDefinitions::containmentAgent),
                entry(
                        SecurityIncidentResponseDefinitions.SECURITY_REPORT_AGENT_ID,
                        SecurityIncidentResponseDefinitions::securityReportAgent));
    }

    public static void main(String[] args) throws Exception {
        new SecurityIncidentResponseGenerator().generateRoot(args, SecurityIncidentResponsePaths::resolveConfigurationRoot);
    }
}
