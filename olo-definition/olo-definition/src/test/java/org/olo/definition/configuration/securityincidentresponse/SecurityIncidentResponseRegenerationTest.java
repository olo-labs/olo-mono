/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.securityincidentresponse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIncidentResponseRegenerationTest {

    @Test
    void regeneratesSecurityIncidentResponseCollection() throws IOException {
        Path configurationRoot = SecurityIncidentResponsePaths.resolveConfigurationRoot();
        new SecurityIncidentResponseGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(SecurityIncidentResponseDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(SecurityIncidentResponseDefinitions.THREAT_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(SecurityIncidentResponseDefinitions.FORENSICS_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(SecurityIncidentResponseDefinitions.CONTAINMENT_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(SecurityIncidentResponseDefinitions.SECURITY_REPORT_AGENT_ID + ".json")).exists();
    }
}
