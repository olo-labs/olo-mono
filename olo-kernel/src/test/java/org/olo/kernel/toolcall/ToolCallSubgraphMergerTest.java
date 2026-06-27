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
                .containsExactlyInAnyOrder("Dyn-Calculator", "Dyn-CPU Usage");
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getSourceNodeId())
                        && mergeResult.entryNodeId().equals(edge.getTargetNodeId()));
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                edge.getSourceNodeId() != null
                        && edge.getSourceNodeId().contains("tool-synthesis")
                        && "end".equals(edge.getTargetNodeId()));
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
