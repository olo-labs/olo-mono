/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.dynamicgraphcreation;

import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the {@code olo-configuration/dynamic-graph-creation/} workflow preset and ensures its
 * planner prompt requires JSON-only structured output.
 */
class DynamicGraphCreationConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void onDiskPresetMatchesDefinition() throws IOException {
        Path configurationRoot = DynamicGraphCreationPaths.resolveConfigurationRoot();
        Path file = configurationRoot.resolve(DynamicGraphCreationDefinitions.FILE_NAME + ".json");
        assertThat(file).as("expected preset file %s", file).exists();

        WorkflowDefinition onDisk = json.deserialize(Files.readString(file));
        WorkflowDefinition expected = DynamicGraphCreationDefinitions.dynamicGraphCreation();

        assertThat(WorkflowValidator.validate(onDisk).valid())
                .as("validation errors for %s", file)
                .isTrue();
        assertThat(onDisk).isEqualTo(expected);
    }

    @Test
    void plannerNodePromptRequiresJsonOnlyStructuredOutput() throws IOException {
        Path configurationRoot = DynamicGraphCreationPaths.resolveConfigurationRoot();
        WorkflowDefinition workflow = json.deserialize(Files.readString(
                configurationRoot.resolve(DynamicGraphCreationDefinitions.FILE_NAME + ".json")));

        NodeDefinition planner = workflow.getNodes().stream()
                .filter(node -> DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(node.getId()))
                .findFirst()
                .orElseThrow();
        Object promptTemplate = planner.getConfiguration().get("promptTemplate");
        assertThat(promptTemplate).isInstanceOf(String.class);
        String prompt = (String) promptTemplate;
        assertThat(prompt).contains("{" + WorkflowPresetInfrastructure.MESSAGE_VARIABLE + "}");
        assertThat(prompt).containsIgnoringCase("Return ONLY a single JSON object");
        assertThat(prompt).containsIgnoringCase("no markdown");
        assertThat(prompt).containsIgnoringCase("no code fences");
        assertThat(prompt).containsIgnoringCase("Respond with JSON only");
        assertThat(prompt).contains("\"nodes\"");
        assertThat(prompt).contains("\"edges\"");
    }

    @Test
    void presetHasPipelinePlannerAndModelInfrastructure() throws IOException {
        Path configurationRoot = DynamicGraphCreationPaths.resolveConfigurationRoot();
        WorkflowDefinition workflow = json.deserialize(Files.readString(
                configurationRoot.resolve(DynamicGraphCreationDefinitions.FILE_NAME + ".json")));

        assertThat(workflow.isEnabled()).isTrue();
        assertThat(workflow.getVariables())
                .anyMatch(variable -> WorkflowPresetInfrastructure.MESSAGE_VARIABLE.equals(variable.getName()));
        assertThat(workflow.getModelProviders()).isNotEmpty();
        assertThat(workflow.getModelRouting()).isNotEmpty();
        assertThat(workflow.getParameters().keySet())
                .contains(
                        AgentWorkflowParameters.SYSTEM_PROMPT,
                        AgentWorkflowParameters.MAX_ITERATIONS,
                        AgentWorkflowParameters.TEMPERATURE);
        assertThat(workflow.getParameters().get(AgentWorkflowParameters.SYSTEM_PROMPT).getDefaultValue())
                .isEqualTo(AgentWorkflowParameters.DEFAULT_SYSTEM_PROMPT);
        assertThat(workflow.getNodes().stream().map(node -> node.getType()).toList())
                .containsExactly("START", "TOOL", "TOOL", "HUMAN", "AGENT", "END");
        assertThat(workflow.getNodes().stream().map(node -> node.getId()).toList())
                .contains(DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID);
        NodeDefinition planner = workflow.getNodes().stream()
                .filter(node -> DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(node.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(planner.getExecutionModel()).isEqualTo(org.olo.definition.execution.ExecutionModel.INLINE);
        assertThat(DynamicGraphPlannerSupport.isDynamicGraphPlanner(planner)).isTrue();
        assertThat(workflow.getVariables())
                .anyMatch(variable -> DynamicGraphPlannerSupport.DEFAULT_OUTPUT_VARIABLE.equals(variable.getName()));

        StudioDesignerAssertions.assertStudioBuildReady(workflow);

        @SuppressWarnings("unchecked")
        var plannerContext = (java.util.Map<String, Object>) workflow.getMetadata().get(PlannerContextDefinition.METADATA_KEY);
        assertThat(plannerContext)
                .containsEntry(
                        PlannerContextDefinition.SELECTED_VARIABLES,
                        java.util.List.of("message", "conversationSummary"));
    }
}
