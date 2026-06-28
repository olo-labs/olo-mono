package org.olo.definition.configuration.researchplanner;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Regenerates {@code olo-configuration/research-planner/*.json}. */
class ResearchPlannerRegenerationTest {

    @Test
    void regeneratesResearchPlannerCollection() throws IOException {
        Path configurationRoot = ResearchPlannerPaths.resolveConfigurationRoot();
        new ResearchPlannerGenerator().generate(configurationRoot);

        assertThat(configurationRoot.resolve(ResearchPlannerDefinitions.ORCHESTRATOR_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ResearchPlannerDefinitions.LITERATURE_AGENT_ID + ".json")).exists();
        assertThat(configurationRoot.resolve(ResearchPlannerDefinitions.SYNTHESIS_AGENT_ID + ".json")).exists();
    }
}
