/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.prompt;

import org.junit.jupiter.api.Test;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.agent.prompt.impl.WorkflowPromptRenderer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowPromptRendererTest {

    private final PromptRenderer renderer = new WorkflowPromptRenderer();

    @Test
    void rendersWorkflowSystemPromptParameter() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("fast")
                .putParameter(
                        AgentWorkflowParameters.SYSTEM_PROMPT,
                        WorkflowParameterDefinition.builder()
                                .type("string")
                                .defaultValue(WorkflowPlannerPromptDefinition.forPreset("fast").getPromptTemplate())
                                .build())
                .nodes(List.of(agentNode("agent")))
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "quick question");

        String rendered = renderer.render(graph, variables);

        assertThat(rendered).contains("quick question");
        assertThat(rendered).contains("high-signal");
    }

    @Test
    void renderTemplateReplacesKnownPlaceholders() {
        assertThat(WorkflowPromptRenderer.renderTemplate(
                        "Hello {message}", Map.of("message", "world")))
                .isEqualTo("Hello world");
    }

    @Test
    void renderForNodeUsesWorkflowSystemPromptParameter() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("agent")
                .putParameter(
                        AgentWorkflowParameters.SYSTEM_PROMPT,
                        WorkflowParameterDefinition.builder()
                                .type("string")
                                .defaultValue("Act like Agent and reply: {message}")
                                .build())
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "Hello");

        var agentNode = org.olo.definition.node.NodeDefinition.builder()
                .id("agent")
                .type("AGENT")
                .build();

        String rendered = renderer.renderForNode(graph, agentNode, variables);

        assertThat(rendered).isEqualTo("Act like Agent and reply: Hello");
    }

    @Test
    void renderForNodePrefersNodePromptTemplate() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("dynamic-graph-creation")
                .putParameter(
                        AgentWorkflowParameters.SYSTEM_PROMPT,
                        WorkflowParameterDefinition.builder()
                                .type("string")
                                .defaultValue("{message}")
                                .build())
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "Hello");

        var plannerNode = NodeDefinition.builder()
                .id("graph-planner")
                .type("AGENT")
                .putConfiguration(DynamicGraphPlannerSupport.CONFIG_DYNAMIC_GRAPH_PLANNER, true)
                .putConfiguration("promptTemplate", "JSON ONLY {message}")
                .build();

        String rendered = renderer.renderForNode(graph, plannerNode, variables);

        assertThat(rendered).isEqualTo("JSON ONLY Hello");
    }

    @Test
    void renderForNodeUsesInlineAgentPromptWhenNoConfiguredPromptExists() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("dynamic-graph-creation")
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "Hello");

        String rendered = renderer.renderForNode(graph, agentNode("greet"), variables);

        assertThat(rendered).contains("Hello");
        assertThat(rendered).contains("autonomous Open LLM Orchestrator (OLO) agent");
    }

    private static NodeDefinition agentNode(String id) {
        return NodeDefinition.builder().id(id).type("AGENT").build();
    }
}
