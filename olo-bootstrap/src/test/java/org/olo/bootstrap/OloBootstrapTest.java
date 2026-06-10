package org.olo.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;

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
        Path presets = Paths.get("../olo-configuration/default").toAbsolutePath().normalize();
        if (!presets.toFile().exists()) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found at " + presets);
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);

        assertThat(registry.getWorkflows()).hasSize(12);
        assertThat(registry.findById("agent")).isPresent();
        assertThat(registry.findByQueue("agent")).isPresent();
    }

    @Test
    void cachesRegistryUntilRefreshRequested() {
        Path presets = Paths.get("../olo-configuration/default").toAbsolutePath().normalize();
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
