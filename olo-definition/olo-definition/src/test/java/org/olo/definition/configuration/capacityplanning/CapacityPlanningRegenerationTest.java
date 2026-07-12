/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.capacityplanning;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityPlanningRegenerationTest {

    @Test
    void regeneratesCapacityPlanningCollection() throws IOException {
        Path configurationRoot = CapacityPlanningPaths.resolveConfigurationRoot();
        new CapacityPlanningGenerator().generate(configurationRoot);
        assertThat(configurationRoot.resolve(CapacityPlanningDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(CapacityPlanningDefinitions.UTILIZATION_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(CapacityPlanningDefinitions.COST_ESTIMATION_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(CapacityPlanningDefinitions.SCALING_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(CapacityPlanningDefinitions.CAPACITY_REPORT_AGENT_ID + ".json")).exists();
    }
}
