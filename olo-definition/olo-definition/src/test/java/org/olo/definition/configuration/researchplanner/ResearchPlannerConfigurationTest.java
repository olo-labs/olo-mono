package org.olo.definition.configuration.researchplanner;

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

class ResearchPlannerConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path configurationRoot = ResearchPlannerPaths.resolveConfigurationRoot();
        assertPreset(configurationRoot, ResearchPlannerDefinitions.ORCHESTRATOR_ID, ResearchPlannerDefinitions.orchestrator());
        assertPreset(configurationRoot, ResearchPlannerDefinitions.LITERATURE_AGENT_ID, ResearchPlannerDefinitions.literatureAgent());
        assertPreset(configurationRoot, ResearchPlannerDefinitions.SYNTHESIS_AGENT_ID, ResearchPlannerDefinitions.synthesisAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsWithoutScenarioTool() throws IOException {
        Path configurationRoot = ResearchPlannerPaths.resolveConfigurationRoot();
        WorkflowDefinition orchestrator = json.deserialize(Files.readString(
                configurationRoot.resolve(ResearchPlannerDefinitions.ORCHESTRATOR_ID + ".json")));

        assertThat(orchestrator.getChildWorkflows()).hasSize(2);
        assertThat(orchestrator.getAvailableAgents()).hasSize(2);
        assertThat(orchestrator.getTools()).isEmpty();
        assertThat(orchestrator.getMetadata()).containsKey(ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION);
        assertThat(orchestrator.getVariables().stream().map(v -> v.getName()))
                .contains(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE);
        assertThat(orchestrator.getEdges()).anyMatch(edge ->
                ResearchPlannerDefinitions.LITERATURE_AGENT_ID.equals(edge.getSourceNodeId())
                        && "agentPlug".equals(edge.getSourcePortId())
                        && ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getTargetNodeId())
                        && "agentPlug".equals(edge.getTargetPortId()));
        assertThat(orchestrator.getEdges()).anyMatch(edge ->
                ResearchPlannerDefinitions.SYNTHESIS_AGENT_ID.equals(edge.getSourceNodeId())
                        && "agentPlug".equals(edge.getSourcePortId()));
        assertThat(orchestrator.getNodes().stream()
                .filter(node -> ResearchPlannerDefinitions.LITERATURE_AGENT_ID.equals(node.getId()))
                .findFirst()
                .orElseThrow()
                .getConfiguration())
                .containsEntry("delegateAgentId", ResearchPlannerDefinitions.LITERATURE_AGENT_ID);
        assertThat(ResearchPlannerDefinitions.literatureAgent().getNodes().stream()
                .filter(node -> "agent".equals(node.getId()))
                .findFirst()
                .orElseThrow()
                .getExecutionModel())
                .isEqualTo(org.olo.definition.execution.ExecutionModel.INLINE);
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
