/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.apiintegrationtriage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApiIntegrationTriageRegenerationTest {

    @Test
    void regeneratesApiIntegrationTriageCollection() throws IOException {
        Path configurationRoot = ApiIntegrationTriagePaths.resolveConfigurationRoot();
        new ApiIntegrationTriageGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(ApiIntegrationTriageDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ApiIntegrationTriageDefinitions.ENDPOINT_PROBE_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ApiIntegrationTriageDefinitions.DEPENDENCY_ANALYSIS_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ApiIntegrationTriageDefinitions.ERROR_CORRELATION_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ApiIntegrationTriageDefinitions.INTEGRATION_REPORT_AGENT_ID + ".json")).exists();
    }
}
