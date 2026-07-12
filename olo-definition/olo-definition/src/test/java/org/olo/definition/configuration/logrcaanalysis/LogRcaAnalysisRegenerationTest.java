/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.logrcaanalysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Regenerates {@code olo-configuration/log-rca-analysis/*.json}. */
class LogRcaAnalysisRegenerationTest {

    @Test
    void regeneratesLogRcaAnalysisCollection() throws IOException {
        Path configurationRoot = LogRcaAnalysisPaths.resolveConfigurationRoot();
        new LogRcaAnalysisGenerator().generate(configurationRoot);

        assertThat(configurationRoot.resolve(LogRcaAnalysisDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LogRcaAnalysisDefinitions.LOG_FAILURE_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LogRcaAnalysisDefinitions.METRICS_RCA_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LogRcaAnalysisDefinitions.CODE_CHANGE_RCA_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(LogRcaAnalysisDefinitions.INCIDENT_SUMMARY_AGENT_ID + ".json")).exists();
    }
}
