package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlannerContextRuntimeTest {

    @Test
    void filtersAvailableToolsWhenSelectedToolsEmpty() {
        WorkflowDefinition graph = WorkflowBuilder.create("Test")
                .id("test")
                .queue("oloQueue2")
                .metadata(PlannerContextDefinition.METADATA_KEY, Map.of(
                        PlannerContextDefinition.SELECTED_TOOLS, List.of(),
                        PlannerContextDefinition.SELECTED_AGENTS, List.of("literature-agent"),
                        PlannerContextDefinition.INJECT_CAPABILITIES, false,
                        PlannerContextDefinition.INJECT_AGENTS, true))
                .build();

        assertThat(AvailableToolsJsonResolver.resolve(graph, "agent")).isEqualTo("[]");
    }

    @Test
    void keepsSelectedAgentsOnly() {
        WorkflowDefinition graph = orchestratorGraph();
        String agentsJson = AvailableAgentsJsonResolver.resolve(graph, "agent");

        assertThat(agentsJson).contains("literature-agent");
        assertThat(agentsJson).doesNotContain("synthesis-agent");
    }

    private static WorkflowDefinition orchestratorGraph() {
        return WorkflowBuilder.create("Orchestrator")
                .id("research-orchestrator")
                .queue("oloQueue2")
                .canvasChildAgentPluginNode("literature-agent", "literature-agent", "Literature Agent")
                .canvasChildAgentPluginNode("synthesis-agent", "synthesis-agent", "Synthesis Agent")
                .connect("literature-agent", "agentPlug", "agent", "agentPlug")
                .connect("synthesis-agent", "agentPlug", "agent", "agentPlug")
                .metadata(PlannerContextDefinition.METADATA_KEY, Map.of(
                        PlannerContextDefinition.SELECTED_TOOLS, List.of(),
                        PlannerContextDefinition.SELECTED_AGENTS, List.of("literature-agent"),
                        PlannerContextDefinition.INJECT_CAPABILITIES, false,
                        PlannerContextDefinition.INJECT_AGENTS, true))
                .build();
    }
}
