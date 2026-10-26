/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.runtime;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeBindingTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void resolvesImplementationClassBeforeId() {
        RuntimeBindingDefinition binding = RuntimeBindingDefinition.builder()
                .implementationClass("com.company.ai.CustomAgentExecutor")
                .implementationId("default-agent-runner")
                .build();

        assertThat(RuntimeBindingResolver.resolve(binding))
                .isEqualTo(RuntimeBindingResolution.IMPLEMENTATION_CLASS);
        assertThat(RuntimeBindingResolver.effectiveRuntime(binding)).isEqualTo(RuntimeKind.JAVA);
    }

    @Test
    void resolvesRegistryIdWhenNoClass() {
        RuntimeBindingDefinition binding = RuntimeBindingDefinition.builder()
                .implementationId("default-agent-runner")
                .build();

        assertThat(RuntimeBindingResolver.resolve(binding))
                .isEqualTo(RuntimeBindingResolution.IMPLEMENTATION_ID);
    }

    @Test
    void resolvesHttpServiceBinding() {
        RuntimeBindingDefinition binding = RuntimeBindingDefinition.builder()
                .runtime(RuntimeKind.HTTP)
                .implementationId("risk-agent-api")
                .build();

        assertThat(RuntimeBindingResolver.resolve(binding))
                .isEqualTo(RuntimeBindingResolution.IMPLEMENTATION_ID);
    }

    @Test
    void roundTripsAgentWithWorkflowRefAlias() throws Exception {
        WorkflowDefinition workflow = WorkflowBuilder.create("Agent Binding")
                .id("technical-analysis")
                .capability(CapabilityDefinition.builder()
                        .name("Technical Analysis")
                        .description("Indicators")
                        .addInput("symbol")
                        .addOutput("analysis")
                        .build())
                .agent(AgentDefinition.builder()
                        .id("technical-analysis")
                        .capability(CapabilityDefinition.builder()
                                .name("Technical Analysis Agent")
                                .description("Analyze trends")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("technical-analysis-v1")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("default-agent-runner")
                                .build())
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .systemPrompt("You are a technical analysis expert.")
                        .build())
                .inputNode("input")
                .outputNode("output")
                .connect("input", "output")
                .build();

        WorkflowValidator.validateOrThrow(workflow);

        String serialized = json.serialize(workflow);
        assertThat(serialized).contains("workflowRef");
        assertThat(serialized).contains("default-agent-runner");

        WorkflowDefinition restored = json.deserialize(serialized);
        AgentDefinition agent = restored.getAgents().get(0);
        assertThat(agent.getWorkflow().getWorkflowId()).isEqualTo("technical-analysis-v1");
        assertThat(agent.getRuntimeBinding().getImplementationId()).isEqualTo("default-agent-runner");
    }




    @Test
    void rejectsEmptyRuntimeBindingOnAFreshWorkflow() {
        var source=org.olo.definition.fresh.FreshWorkflows.chat("binding-test");
        assertThat(WorkflowValidator.validate(source).valid()).isTrue();
        var changed=source.toBuilder().runtimeBinding(RuntimeBindingDefinition.builder().build()).build();
        var result=WorkflowValidator.validate(changed);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(error->error.contains("runtimeBinding"));
    }
}
