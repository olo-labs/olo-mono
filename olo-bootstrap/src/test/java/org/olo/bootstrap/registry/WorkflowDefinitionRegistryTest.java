package org.olo.bootstrap.registry;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

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

    private static CachedWorkflowDefinition cached(String sourcePath, WorkflowDefinition definition) {
        return new CachedWorkflowDefinition(sourcePath, definition);
    }

    private static WorkflowDefinition workflow(String id, String version, String queue) {
        return workflow(id, version, queue, null);
    }

    private static WorkflowDefinition workflow(String id, String version, String queue, Boolean isDefault) {
        WorkflowDefinition.Builder builder = WorkflowDefinition.builder()
                .id(id)
                .version(version)
                .queue(queue);
        if (isDefault != null) {
            builder.isDefault(isDefault);
        }
        return builder.build();
    }
}
