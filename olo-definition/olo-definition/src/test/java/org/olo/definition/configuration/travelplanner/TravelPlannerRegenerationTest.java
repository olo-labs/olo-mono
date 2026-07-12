/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.travelplanner;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Regenerates {@code olo-configuration/travel-planner/*.json}. */
class TravelPlannerRegenerationTest {

    @Test
    void regeneratesTravelPlannerCollection() throws IOException {
        Path configurationRoot = TravelPlannerPaths.resolveConfigurationRoot();
        new TravelPlannerGenerator().generate(configurationRoot);

        assertThat(configurationRoot.resolve(TravelPlannerDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(TravelPlannerDefinitions.DESTINATION_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(TravelPlannerDefinitions.ITINERARY_AGENT_ID + ".json")).exists();
    }
}
