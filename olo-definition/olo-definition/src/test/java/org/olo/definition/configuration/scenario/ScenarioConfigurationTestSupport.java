/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario;

import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public final class ScenarioConfigurationTestSupport {

    private ScenarioConfigurationTestSupport() {
    }

    public static void assertPreset(Path configurationRoot, String workflowId, WorkflowDefinition expected)
            throws IOException {
        JsonWorkflowSerializer json = new JsonWorkflowSerializer();
        Path file = configurationRoot.resolve(workflowId + ".json");
        assertThat(file).as("expected preset file %s", file).exists();
        WorkflowDefinition onDisk = json.deserialize(Files.readString(file));
        assertThat(WorkflowValidator.validate(onDisk).valid())
                .as("validation errors for %s", file)
                .isTrue();
        assertThat(onDisk).isEqualTo(expected);
    }

    public static void assertOrchestratorChildAgents(
            Path configurationRoot,
            String orchestratorId,
            int expectedChildCount,
            String... childAgentIds) throws IOException {
        JsonWorkflowSerializer json = new JsonWorkflowSerializer();
        WorkflowDefinition orchestrator = json.deserialize(Files.readString(
                configurationRoot.resolve(orchestratorId + ".json")));
        assertThat(orchestrator.getChildWorkflows()).hasSize(expectedChildCount);
        assertThat(orchestrator.getAvailableAgents()).hasSize(expectedChildCount);
        assertThat(orchestrator.getMetadata()).containsKey(ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION);
        for (String childAgentId : childAgentIds) {
            assertThat(orchestrator.getEdges()).anyMatch(edge ->
                    childAgentId.equals(edge.getSourceNodeId())
                            && "agentPlug".equals(edge.getSourcePortId())
                            && ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getTargetNodeId()));
        }
        StudioDesignerAssertions.assertStudioBuildReady(orchestrator);
    }
}
