/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.parameter;

import org.olo.definition.runtime.AgentDelegationPolicy;
import org.olo.definition.planner.AgentAvailableAgents;
import org.olo.definition.planner.WorkflowPlannerMetadata;
import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.ValidationTestFixtures;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWorkflowParametersTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void builderAppliesAgentParameterDefaults() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Agent")
                .id("agent")
                .capability(ValidationTestFixtures.minimalCapability())
                .agentParameters()
                .build();

        assertThat(workflow.getParameters()).containsOnlyKeys(
                AgentWorkflowParameters.MAX_ITERATIONS,
                AgentWorkflowParameters.SYSTEM_PROMPT,
                AgentWorkflowParameters.MODEL,
                AgentWorkflowParameters.TEMPERATURE);
        assertThat(workflow.getParameters().get(AgentWorkflowParameters.MAX_ITERATIONS).getDefaultValue())
                .isEqualTo(10);
        assertThat(workflow.getParameters().get(AgentWorkflowParameters.TEMPERATURE).getDefaultValue())
                .isEqualTo(0.2);
        assertThat(workflow.getParameters().get(AgentWorkflowParameters.SYSTEM_PROMPT).getDefaultValue())
                .isEqualTo(WorkflowPlannerPromptDefinition.forPreset("agent").getPromptTemplate());

        WorkflowParameterDefinition temperature =
                workflow.getParameters().get(AgentWorkflowParameters.TEMPERATURE);
        assertThat(temperature.getType()).isEqualTo("number");
        assertThat(temperature.getLabel()).isEqualTo("Temperature");
        assertThat(temperature.getMinimum()).isEqualTo(0d);
        assertThat(temperature.getMaximum()).isEqualTo(2d);
        assertThat(temperature.getStep()).isEqualTo(0.1d);
        assertThat(temperature.getUi().getWidget()).isEqualTo("SLIDER");
    }

    @Test
    void normalizesLegacyWidgetNamesOnDeserialize() throws Exception {
        String raw = """
                {
                  "id": "agent",
                  "capability": {
                    "name": "Agent",
                    "description": "test",
                    "tags": [],
                    "examples": [],
                    "required_inputs": [],
                    "required_outputs": [],
                    "required_context": []
                  },
                  "parameters": {
                    "temperature": {
                      "type": "number",
                      "ui": { "widget": "slider" }
                    }
                  }
                }
                """;

        WorkflowDefinition workflow = json.deserialize(raw);
        assertThat(workflow.getParameters().get("temperature").getUi().getWidget()).isEqualTo("SLIDER");
    }

    @Test
    void deserializesShorthandParameterLiterals() throws Exception {
        String raw = """
                {
                  "id": "agent",
                  "capability": {
                    "name": "Agent",
                    "description": "test",
                    "tags": [],
                    "examples": [],
                    "required_inputs": [],
                    "required_outputs": [],
                    "required_context": []
                  },
                  "parameters": {
                    "maxIterations": 10,
                    "systemPrompt": "",
                    "model": "",
                    "temperature": 0.2
                  }
                }
                """;

        WorkflowDefinition workflow = json.deserialize(raw);
        assertThat(workflow.getParameters().get("maxIterations").getType()).isEqualTo("integer");
        assertThat(workflow.getParameters().get("maxIterations").getDefaultValue()).isEqualTo(10);
        assertThat(workflow.getParameters().get("systemPrompt").getType()).isEqualTo("string");
        assertThat(workflow.getParameters().get("temperature").getDefaultValue()).isEqualTo(0.2);
    }



    @Test
    void deserializesChildWorkflowIdShorthand() throws Exception {
        String raw = """
                {
                  "id": "agent",
                  "capability": {
                    "name": "Agent",
                    "description": "test",
                    "tags": [],
                    "examples": [],
                    "required_inputs": [],
                    "required_outputs": [],
                    "required_context": []
                  },
                  "childWorkflows": ["planner", "research-agent"]
                }
                """;

        WorkflowDefinition workflow = json.deserialize(raw);
        assertThat(workflow.getChildWorkflows()).hasSize(2);
        assertThat(workflow.getChildWorkflows().get(0).getWorkflowId()).isEqualTo("planner");
        assertThat(workflow.getChildWorkflows().get(1).getWorkflowId()).isEqualTo("research-agent");
    }
}
