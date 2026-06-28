package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class AgentLabelResolverTest {

    @Test
    void resolvesLabelFromCanvasAgentNode() {
        WorkflowDefinition graph = WorkflowBuilder.create("Orchestrator")
                .id("research-orchestrator")
                .queue("oloQueue2")
                .addNode(org.olo.definition.node.NodeDefinition.builder()
                        .id("synthesis-agent")
                        .type(NodeType.AGENT.name())
                        .label("Synthesis Agent")
                        .putConfiguration("delegateAgentId", "synthesis-agent")
                        .build())
                .build();

        assertThat(AgentLabelResolver.resolve("synthesis-agent", graph, ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID))
                .isEqualTo("Synthesis Agent");
    }
}
