/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.travelplanner;

import org.junit.jupiter.api.Test;
import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlannerConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path configurationRoot = TravelPlannerPaths.resolveConfigurationRoot();
        assertPreset(configurationRoot, TravelPlannerDefinitions.ORCHESTRATOR_ID, TravelPlannerDefinitions.orchestrator());
        assertPreset(configurationRoot, TravelPlannerDefinitions.DESTINATION_AGENT_ID, TravelPlannerDefinitions.destinationAgent());
        assertPreset(configurationRoot, TravelPlannerDefinitions.ITINERARY_AGENT_ID, TravelPlannerDefinitions.itineraryAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndScenarioTools() throws IOException {
        Path configurationRoot = TravelPlannerPaths.resolveConfigurationRoot();
        WorkflowDefinition orchestrator = json.deserialize(Files.readString(
                configurationRoot.resolve(TravelPlannerDefinitions.ORCHESTRATOR_ID + ".json")));

        assertThat(orchestrator.getChildWorkflows()).hasSize(2);
        assertThat(orchestrator.getAvailableAgents()).hasSize(2);
        assertThat(orchestrator.getTools()).extracting(tool -> tool.getRuntimeBinding().getImplementationId())
                .contains(
                        TravelPlannerDefinitions.DESTINATIONS_TOOL_ID,
                        TravelPlannerDefinitions.OFFERS_TOOL_ID);
        assertThat(orchestrator.getMetadata()).containsKey(ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION);
        assertThat(orchestrator.getVariables().stream().map(v -> v.getName()))
                .contains(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE);
        assertThat(orchestrator.getEdges()).anyMatch(edge ->
                TravelPlannerDefinitions.DESTINATION_AGENT_ID.equals(edge.getSourceNodeId())
                        && "agentPlug".equals(edge.getSourcePortId())
                        && ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getTargetNodeId()));
        assertThat(orchestrator.getEdges()).anyMatch(edge ->
                TravelPlannerDefinitions.ITINERARY_AGENT_ID.equals(edge.getSourceNodeId())
                        && "agentPlug".equals(edge.getSourcePortId()));
        assertThat(orchestrator.getNodes().stream()
                .filter(node -> TravelPlannerDefinitions.DESTINATION_AGENT_ID.equals(node.getId()))
                .findFirst()
                .orElseThrow()
                .getConfiguration())
                .containsEntry("delegateAgentId", TravelPlannerDefinitions.DESTINATION_AGENT_ID);
        StudioDesignerAssertions.assertStudioBuildReady(orchestrator);
    }

    private void assertPreset(Path configurationRoot, String workflowId, WorkflowDefinition expected) throws IOException {
        Path file = configurationRoot.resolve(workflowId + ".json");
        assertThat(file).as("expected preset file %s", file).exists();
        WorkflowDefinition onDisk = json.deserialize(Files.readString(file));
        assertThat(WorkflowValidator.validate(onDisk).valid())
                .as("validation errors for %s", file)
                .isTrue();
        assertThat(onDisk).isEqualTo(expected);
    }
}
