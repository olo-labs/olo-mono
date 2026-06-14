package org.olo.kernel.agent.prompt;

import org.junit.jupiter.api.Test;
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
    void rendersDefaultPromptWithMessageVariable() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("fast")
                .defaultPromptId(WorkflowPlannerPromptDefinition.DEFAULT_PROMPT_ID)
                .prompts(List.of(WorkflowPlannerPromptDefinition.forPreset("fast")))
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "quick question");

        String rendered = renderer.render(graph, variables);

        assertThat(rendered).contains("quick question");
        assertThat(rendered).doesNotContain("{message}");
    }

    @Test
    void renderTemplateReplacesKnownPlaceholders() {
        assertThat(WorkflowPromptRenderer.renderTemplate(
                        "Hello {message}", Map.of("message", "world")))
                .isEqualTo("Hello world");
    }

    @Test
    void renderForNodeUsesInlineAgentPromptForNonPlannerAgents() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("dynamic-graph-creation")
                .defaultPromptId(WorkflowPlannerPromptDefinition.DEFAULT_PROMPT_ID)
                .prompts(List.of(WorkflowPlannerPromptDefinition.builder()
                        .id(WorkflowPlannerPromptDefinition.DEFAULT_PROMPT_ID)
                        .name("planner")
                        .promptTemplate("JSON ONLY {message}")
                        .build()))
                .build();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);
        variables.set("message", "Hello");

        var agentNode = org.olo.definition.node.NodeDefinition.builder()
                .id("greet")
                .type("AGENT")
                .build();

        String rendered = renderer.renderForNode(graph, agentNode, variables);

        assertThat(rendered).contains("Hello");
        assertThat(rendered).contains("helpful assistant");
        assertThat(rendered).doesNotContain("JSON ONLY");
    }
}
