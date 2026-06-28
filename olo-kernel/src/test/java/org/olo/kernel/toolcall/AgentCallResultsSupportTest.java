package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCallResultsSupportTest {

    @Test
    void filtersAlreadyCompletedAgents() {
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(baseWorkflow());
        variables.set(
                ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE,
                """
                [{"agentId":"literature-agent","response":"done"}]
                """);

        List<ToolCallSubgraphMerger.ParsedAgentCall> pending = AgentCallResultsSupport.filterPending(
                List.of(
                        new ToolCallSubgraphMerger.ParsedAgentCall("literature-agent", "again"),
                        new ToolCallSubgraphMerger.ParsedAgentCall("synthesis-agent", "summarize")),
                variables);

        assertThat(pending).containsExactly(new ToolCallSubgraphMerger.ParsedAgentCall("synthesis-agent", "summarize"));
    }

    @Test
    void stripInjectedNodesRestoresPlannerToEnd() {
        WorkflowDefinition expanded = ToolCallSubgraphMerger.mergeAgentAndToolCalls(
                baseWorkflow(),
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                List.of(new ToolCallSubgraphMerger.ParsedAgentCall("literature-agent", "find papers")),
                List.of()).graph();

        assertThat(expanded.getNodes().stream().map(node -> node.getId()))
                .anyMatch(DynamicSubgraphStripper::isInjectedNodeId);

        WorkflowDefinition stripped = DynamicSubgraphStripper.stripInjectedNodes(
                expanded, ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "end");

        assertThat(stripped.getNodes().stream().map(node -> node.getId()))
                .noneMatch(DynamicSubgraphStripper::isInjectedNodeId);
        assertThat(stripped.getEdges()).anyMatch(edge ->
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getSourceNodeId())
                        && "end".equals(edge.getTargetNodeId()));
    }

    private static WorkflowDefinition baseWorkflow() {
        return WorkflowBuilder.create("Agent")
                .id("agent")
                .queue("oloQueue2")
                .startNode("start")
                .addNode(org.olo.definition.node.NodeDefinition.builder()
                        .id(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID)
                        .type(NodeType.AGENT.name())
                        .executionKind(ExecutionKind.ACTIVITY)
                        .executionModel(ExecutionModel.INLINE)
                        .putConfiguration(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true)
                        .build())
                .endNode("end")
                .connect("start", "out", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "in")
                .connect(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "out", "end", "in")
                .build();
    }
}
