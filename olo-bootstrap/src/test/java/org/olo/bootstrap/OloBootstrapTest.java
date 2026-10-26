/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
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

        assertThat(registry.getWorkflows()).hasSize(11);
        assertThat(registry.findById("agent")).isPresent();
        assertThat(registry.findById("agent")).isPresent();

        WorkflowDefinition agent = registry.findById("agent").orElseThrow();
        assertThat(agent.getVersion()).isEqualTo("1.0.0");
        assertThat(agent.isDefault()).isTrue();
        assertThat(registry.findByIdAndVersion("agent", "1.0.0")).contains(agent);
        assertThat(registry.findByIdAndVersion("agent", "9.9.9")).contains(agent);
        assertThat(registry.getWorkflowsByIdAndVersion()).containsKey("agent@1.0.0");
        assertThat(agent.getParameters()).isEmpty();
        assertThat(agent.getChildWorkflows()).isEmpty();
        assertThat(agent.getAvailableAgentIds()).isEmpty();
        assertThat(agent.getRuntime().getExecutionModel().name()).isEqualTo("INLINE");
        assertThat(agent.getNodes()).extracting(node -> node.getType()).containsExactly("START", "AGENT", "END");
        assertThat(agent.getNodes().get(1).getConfiguration()).containsEntry("maxToolRounds", 0);

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
