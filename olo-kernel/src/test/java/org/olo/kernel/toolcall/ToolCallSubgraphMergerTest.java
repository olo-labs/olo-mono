package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallSubgraphMergerTest {

    @Test
    void validatesToolCallSequenceJson() {
        var result = ToolCallSubgraphMerger.validate(
                """
                {
                  "toolCalls": [
                    { "toolId": "olo-core:calculator", "arguments": { "a": 1, "b": 2 } }
                  ],
                  "directResponse": null
                }
                """,
                """
                [{"toolId":"olo-core:calculator"}]
                """);

        assertThat(result.valid()).isTrue();
        assertThat(result.kind()).isEqualTo(ToolCallSubgraphMerger.ValidationResult.Kind.TOOL_CALLS);
        assertThat(result.toolCalls()).hasSize(1);
    }

    @Test
    void acceptsDirectResponseWithoutTools() {
        var result = ToolCallSubgraphMerger.validate(
                """
                { "toolCalls": [], "directResponse": "Hello" }
                """,
                "[]");

        assertThat(result.valid()).isTrue();
        assertThat(result.kind()).isEqualTo(ToolCallSubgraphMerger.ValidationResult.Kind.DIRECT_RESPONSE);
        assertThat(result.directResponse()).isEqualTo("Hello");
    }

    @Test
    void rejectsToolIdOutsideAllowList() {
        var result = ToolCallSubgraphMerger.validate(
                """
                {
                  "toolCalls": [
                    { "toolId": "olo-core:unknown", "arguments": {} }
                  ],
                  "directResponse": null
                }
                """,
                """
                [{"toolId":"olo-core:calculator"}]
                """);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("allow-list");
    }

    @Test
    void validatesAgentCallsAgainstAllowList() {
        var result = ToolCallSubgraphMerger.validate(
                """
                {
                  "toolCalls": [],
                  "agentCalls": [
                    { "agentId": "literature-agent", "message": "Find papers on OLO" }
                  ],
                  "directResponse": null
                }
                """,
                "[]",
                """
                [{"agentId":"literature-agent"}]
                """);

        assertThat(result.valid()).isTrue();
        assertThat(result.agentCalls()).hasSize(1);
        assertThat(result.agentCalls().getFirst().agentId()).isEqualTo("literature-agent");
    }

    @Test
    void mergeAgentCallsRoutesEachAgentAsChildWorkflowToEnd() {
        WorkflowDefinition base = orchestratorWithChildAgents();
        ToolCallSubgraphMerger.MergeResult mergeResult = ToolCallSubgraphMerger.mergeAgentAndToolCalls(
                base,
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                List.of(
                        new ToolCallSubgraphMerger.ParsedAgentCall("literature-agent", "Find papers"),
                        new ToolCallSubgraphMerger.ParsedAgentCall("synthesis-agent", "Summarize findings")),
                List.of());

        assertThat(mergeResult.entryNodeId()).contains("step-0");
        assertThat(mergeResult.graph().getNodes().stream()
                .filter(node -> node.getId() != null && node.getId().contains("agent-dyn-"))
                .map(node -> node.getLabel())
                .filter(label -> label != null && !label.isBlank()))
                .containsExactly("Dyn-Agent Literature Agent", "Dyn-Agent Synthesis Agent");
        assertThat(mergeResult.graph().getNodes().stream()
                .filter(node -> node.getId().contains("step-0"))
                .findFirst()
                .orElseThrow()
                .getExecutionModel())
                .isEqualTo(org.olo.definition.execution.ExecutionModel.CHILD_WORKFLOW);
        assertThat(mergeResult.graph().getEdges()).noneMatch(edge ->
                edge.getTargetNodeId() != null && edge.getTargetNodeId().contains("agent-synthesis"));
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                edge.getSourceNodeId() != null
                        && edge.getSourceNodeId().contains("step-1")
                        && "end".equals(edge.getTargetNodeId()));
    }

    @Test
    void mergesToolChainWithSynthesisBeforeEnd() {
        WorkflowDefinition base = baseWorkflowWithTools();
        ToolCallSubgraphMerger.MergeResult mergeResult = ToolCallSubgraphMerger.merge(
                base,
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                java.util.List.of(
                        new ToolCallSubgraphMerger.ParsedToolCall("olo-core:calculator", java.util.Map.of()),
                        new ToolCallSubgraphMerger.ParsedToolCall("olo-core:cpu-usage", java.util.Map.of())));

        assertThat(mergeResult.graph().getNodes().stream().map(node -> node.getType()))
                .contains("TOOL", "AGENT");
        assertThat(mergeResult.graph().getNodes().stream()
                .filter(node -> "TOOL".equals(node.getType()))
                .map(node -> node.getLabel())
                .filter(label -> label != null && !label.isBlank()))
                .containsExactlyInAnyOrder("Dyn-Tool Calculator", "Dyn-Tool CPU Usage");
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getSourceNodeId())
                        && mergeResult.entryNodeId().equals(edge.getTargetNodeId()));
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                edge.getSourceNodeId() != null
                        && edge.getSourceNodeId().contains("tool-synthesis")
                        && "end".equals(edge.getTargetNodeId()));
    }

    private static WorkflowDefinition orchestratorWithChildAgents() {
        return WorkflowBuilder.from(baseWorkflow())
                .childWorkflow(org.olo.definition.workflow.ChildWorkflowDefinition.builder()
                        .workflowId("literature-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(org.olo.definition.workflow.ChildWorkflowDefinition.builder()
                        .workflowId("synthesis-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .addNode(org.olo.definition.node.NodeDefinition.builder()
                        .id("literature-agent")
                        .type(NodeType.AGENT.name())
                        .label("Literature Agent")
                        .putConfiguration("delegateAgentId", "literature-agent")
                        .build())
                .addNode(org.olo.definition.node.NodeDefinition.builder()
                        .id("synthesis-agent")
                        .type(NodeType.AGENT.name())
                        .label("Synthesis Agent")
                        .putConfiguration("delegateAgentId", "synthesis-agent")
                        .build())
                .connect("literature-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .connect("synthesis-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .build();
    }

    private static WorkflowDefinition baseWorkflowWithTools() {
        return WorkflowBuilder.from(baseWorkflow())
                .tool(ToolDefinition.builder()
                        .id("calculator")
                        .capability(CapabilityDefinition.builder()
                                .name("Calculator")
                                .description("Calculator tool")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("olo-core:calculator")
                                .build())
                        .build())
                .tool(ToolDefinition.builder()
                        .id("cpu-usage")
                        .capability(CapabilityDefinition.builder()
                                .name("CPU Usage")
                                .description("CPU usage tool")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("olo-core:cpu-usage")
                                .build())
                        .build())
                .build();
    }

    private static WorkflowDefinition baseWorkflow() {
        return WorkflowBuilder.create("Agent")
                .id("agent")
                .queue("agent")
                .startNode("start")
                .addNode(plannerNode())
                .endNode("end")
                .connect("start", "out", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "in")
                .connect(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "out", "end", "in")
                .build();
    }

    private static NodeDefinition plannerNode() {
        return NodeDefinition.builder()
                .id(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID)
                .type(NodeType.AGENT.name())
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .putConfiguration(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true)
                .build();
    }
}
