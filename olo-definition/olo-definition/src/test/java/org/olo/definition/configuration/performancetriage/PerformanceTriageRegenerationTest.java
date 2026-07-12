/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.performancetriage;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceTriageRegenerationTest {

    @Test
    void regeneratesPerformanceTriageCollection() throws IOException {
        Path configurationRoot = PerformanceTriagePaths.resolveConfigurationRoot();
        new PerformanceTriageGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(PerformanceTriageDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(PerformanceTriageDefinitions.LATENCY_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(PerformanceTriageDefinitions.RESOURCE_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(PerformanceTriageDefinitions.TUNING_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(PerformanceTriageDefinitions.REPORT_AGENT_ID + ".json")).exists();
    }
}
