/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowValidatorRuntimeTest {

    @Test
    void materializesRuntimeWhenJsonOmitsRuntimeBlock() throws Exception {
        WorkflowDefinition workflow =
                new JsonWorkflowSerializer()
                        .deserialize(
                                """
                                {
                                  "id": "planner",
                                  "capability": {
                                    "name": "Planner",
                                    "description": "Planner",
                                    "required_inputs": ["input"],
                                    "required_outputs": ["output"]
                                  },
                                  "nodes": []
                                }
                                """);

        assertThat(workflow.getRuntime()).isNotNull();
        assertThat(workflow.getRuntime().getContractVersion()).isEqualTo("1.0");
        assertThat(workflow.getRuntime().getExecutionModel()).isEqualTo(ExecutionModel.INLINE);
    }

    @Test
    void materializesExecutionModelWhenJsonRuntimeOmitsIt() throws Exception {
        WorkflowDefinition workflow =
                new JsonWorkflowSerializer()
                        .deserialize(
                                """
                                {
                                  "id": "agent",
                                  "capability": {
                                    "name": "Agent",
                                    "description": "Agent",
                                    "required_inputs": ["input"],
                                    "required_outputs": ["output"]
                                  },
                                  "runtime": {
                                    "contractVersion": "1.0",
                                    "capabilities": ["DEBUG"]
                                  },
                                  "nodes": []
                                }
                                """);

        assertThat(workflow.getRuntime().getExecutionModel()).isEqualTo(ExecutionModel.INLINE);
        assertThat(workflow.getRuntime().getCapabilities()).containsExactly(RuntimeCapability.DEBUG);
    }

    @Test
    void rejectsInvalidWorkflowRuntimeDefaultTimeout() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("agent")
                .capability(ValidationTestFixtures.minimalCapability())
                .runtime(WorkflowRuntimeDefinition.builder()
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .defaultTimeout("not-a-duration")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("runtime.defaultTimeout"));
    }

    @Test
    void rejectsAgentRegistryWithoutChildWorkflowExecutionModel() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("agent-registry")
                .capability(ValidationTestFixtures.minimalCapability())
                .addAgent(AgentDefinition.builder()
                        .id("research-agent")
                        .capability(CapabilityDefinition.builder()
                                .name("Research Agent")
                                .description("Research")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("research-agent")
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("runtime.executionModel CHILD_WORKFLOW"));
    }

    @Test
    void rejectsAgentNodeWithoutChildWorkflowExecutionModel() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("agent-node")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("child")
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("execution.executionModel CHILD_WORKFLOW"));
    }

    @Test
    void acceptsLeafSelfAgentWithInlineExecution() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("literature-agent")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.ACTIVITY)
                        .executionModel(ExecutionModel.INLINE)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("literature-agent")
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isTrue();
    }
}
