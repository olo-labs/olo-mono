package org.olo.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class OloBootstrapTest {

    @AfterEach
    void tearDown() {
        OloBootstrap.reset();
    }

    @Test
    void loadsOloConfigurationPresets() {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!presets.toFile().exists()) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found at " + presets);
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);

        assertThat(registry.getWorkflows()).hasSize(12);
        assertThat(registry.findById("agent")).isPresent();
        assertThat(registry.findById("agent")).isPresent();

        WorkflowDefinition agent = registry.findById("agent").orElseThrow();
        assertThat(agent.getVersion()).isEqualTo("1.0.0");
        assertThat(agent.isDefault()).isTrue();
        assertThat(registry.findByIdAndVersion("agent", "1.0.0")).contains(agent);
        assertThat(registry.findByIdAndVersion("agent", "9.9.9")).contains(agent);
        assertThat(registry.getWorkflowsByIdAndVersion()).containsKey("agent@1.0.0");
        assertThat(agent.getParameters()).containsKeys(
                "maxIterations", "systemPrompt", "model", "temperature");
        assertThat(agent.getMetadata()).containsEntry("role", "agent")
                .containsEntry("agentType", "autonomous")
                .containsEntry("planningStrategy", "react")
                .containsEntry("agentSelectionStrategy", "dynamic");
        assertThat(agent.getChildWorkflows()).hasSize(4);
        assertThat(agent.getAvailableAgentIds()).containsExactly("planner", "reviewer", "architect");
        assertThat(agent.getRuntime().getDelegation().getEnabled()).isTrue();
        assertThat(agent.getRuntime().getDelegation().getParallelEnabled()).isTrue();
        assertThat(agent.getRuntime().getDelegation().getMaxDepth()).isEqualTo(3);
        assertThat(agent.getRuntime().getDelegation().getMaxDelegations()).isEqualTo(10);
        assertThat(agent.getRuntime().getDelegation().getResultAggregation().name()).isEqualTo("MERGE");
        assertThat(agent.getRuntime().getDelegation().getMemoryScope().name()).isEqualTo("SHARED");
        assertThat(agent.getTools()).isEmpty();
    }

    @Test
    void cachesRegistryUntilRefreshRequested() {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!presets.toFile().exists()) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found at " + presets);
        }

        WorkflowDefinitionRegistry first = OloBootstrap.load(presets, false);
        assertThat(OloBootstrap.load(presets, false)).isSameAs(first);

        WorkflowDefinitionRegistry refreshed = OloBootstrap.load(presets, false, true);
        assertThat(refreshed).isNotSameAs(first);
        assertThat(refreshed.findById("agent")).isPresent();
        assertThat(OloBootstrap.load(presets, false)).isSameAs(refreshed);
    }
}
