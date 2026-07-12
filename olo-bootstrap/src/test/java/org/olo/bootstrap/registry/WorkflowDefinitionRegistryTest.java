/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.bootstrap.registry;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinitionBuilder;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDefinitionRegistryTest {

    @Test
    void indexesAllVersionsAndResolvesLatestWhenNoDefaultMarked() {
        WorkflowDefinition v1 = workflow("demo", "1.0.0", "demo");
        WorkflowDefinition v2 = workflow("demo", "2.0.0", "demo");

        WorkflowDefinitionRegistry registry = WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(
                        cached("v1.json", v1),
                        cached("v2.json", v2)));

        assertThat(registry.getWorkflows()).hasSize(2);
        assertThat(registry.getWorkflowsByIdAndVersion()).hasSize(2)
                .containsKeys("demo@1.0.0", "demo@2.0.0");

        assertThat(registry.findByIdAndVersion("demo", "1.0.0")).contains(v1);
        assertThat(registry.findByIdAndVersion("demo", "2.0.0")).contains(v2);
        assertThat(registry.findById("demo")).contains(v2);
        assertThat(registry.findByQueue("demo")).contains(v2);
    }

    @Test
    void prefersDefaultWorkspaceAndFallsBackWhenVersionMissing() {
        WorkflowDefinition v1 = workflow("demo", "1.0.0", "demo", true);
        WorkflowDefinition v2 = workflow("demo", "2.0.0", "demo", false);

        WorkflowDefinitionRegistry registry = WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(cached("v1.json", v1), cached("v2.json", v2)));

        assertThat(registry.findById("demo")).contains(v1);
        assertThat(registry.findByQueue("demo")).contains(v1);
        assertThat(registry.findDefaultById("demo")).contains(v1);
        assertThat(registry.findByIdAndVersion("demo", "2.0.0")).contains(v2);
        assertThat(registry.findByIdAndVersion("demo", "9.9.9")).contains(v1);
    }

    @Test
    void keepsOlderVersionAvailableWhenQueueDiffersByVersion() {
        WorkflowDefinition v1 = workflow("demo", "1.0.0", "demo-1.0.0");
        WorkflowDefinition v2 = workflow("demo", "2.0.0", "demo-2.0.0");

        WorkflowDefinitionRegistry registry = WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(cached("v1.json", v1), cached("v2.json", v2)));

        assertThat(registry.findByIdAndVersion("demo", "1.0.0")).contains(v1);
        assertThat(registry.findByIdAndVersion("demo", "2.0.0")).contains(v2);
        assertThat(registry.findByQueue("demo-1.0.0")).contains(v1);
        assertThat(registry.findByQueue("demo-2.0.0")).contains(v2);
        assertThat(registry.getWorkflowsByQueue()).hasSize(2);
    }

    @Test
    void resolvesSharedQueueByWorkflowId() {
        WorkflowDefinition agent = workflow("agent", "1.0.0", "oloQueue2", true);
        WorkflowDefinition dynamic = workflow("dynamic-graph-creation", "1.0.0", "oloQueue2", true);

        WorkflowDefinitionRegistry registry = WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(cached("agent.json", agent), cached("dynamic-graph-creation.json", dynamic)));

        assertThat(registry.findByQueue("oloQueue2")).contains(agent);
        assertThat(registry.resolve("oloQueue2", "dynamic-graph-creation")).contains(dynamic);
        assertThat(registry.resolve("oloQueue2", "agent")).contains(agent);
    }

    @Test
    void resolvesWorkflowTypeFromPrimaryDefinitionOnQueue() {
        WorkflowDefinition agent = WorkflowDefinition.builder()
                .id("agent")
                .version("1.0.0")
                .queue("oloQueue2")
                .isDefault(true)
                .workflowType("olo")
                .build();
        WorkflowDefinitionRegistry registry = WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(cached("agent.json", agent)));

        assertThat(registry.resolveWorkflowTypeForQueue("oloQueue2")).isEqualTo("olo");
        assertThat(registry.resolveWorkflowTypeForQueue("missing")).isEqualTo("olo");
    }

    private static CachedWorkflowDefinition cached(String sourcePath, WorkflowDefinition definition) {
        return new CachedWorkflowDefinition(sourcePath, definition);
    }

    private static WorkflowDefinition workflow(String id, String version, String queue) {
        return workflow(id, version, queue, null);
    }

    private static WorkflowDefinition workflow(String id, String version, String queue, Boolean isDefault) {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .id(id)
                .version(version)
                .queue(queue);
        if (isDefault != null) {
            builder.isDefault(isDefault);
        }
        return builder.build();
    }
}
