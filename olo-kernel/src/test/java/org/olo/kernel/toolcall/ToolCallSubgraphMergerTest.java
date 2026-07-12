/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.ToolCallValidationResult;
import org.olo.kernel.toolcall.support.ToolCallSubgraphMergerTestWorkflows;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallSubgraphMergerTest {

    private static final ToolCallSubgraphMerger MERGER = ToolCallFactories.defaultToolCallSubgraphMerger();

    @Test
    void validatesToolCallSequenceJson() {
        var result = MERGER.validate(
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
        assertThat(result.kind()).isEqualTo(ToolCallValidationResult.Kind.TOOL_CALLS);
        assertThat(result.toolCalls()).hasSize(1);
    }

    @Test
    void acceptsDirectResponseWithoutTools() {
        var result = MERGER.validate(
                """
                { "toolCalls": [], "directResponse": "Hello" }
                """,
                "[]");

        assertThat(result.valid()).isTrue();
        assertThat(result.kind()).isEqualTo(ToolCallValidationResult.Kind.DIRECT_RESPONSE);
        assertThat(result.directResponse()).isEqualTo("Hello");
    }

    @Test
    void rejectsToolIdOutsideAllowList() {
        var result = MERGER.validate(
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
        var result = MERGER.validate(
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
        WorkflowDefinition base = ToolCallSubgraphMergerTestWorkflows.orchestratorWithChildAgents();
        var mergeResult = MERGER.mergeAgentAndToolCalls(
                base,
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                List.of(
                        new ParsedAgentCall("literature-agent", "Find papers"),
                        new ParsedAgentCall("synthesis-agent", "Summarize findings")),
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
                .isEqualTo(ExecutionModel.CHILD_WORKFLOW);
        assertThat(mergeResult.graph().getEdges()).noneMatch(edge ->
                edge.getTargetNodeId() != null && edge.getTargetNodeId().contains("agent-synthesis"));
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                edge.getSourceNodeId() != null
                        && edge.getSourceNodeId().contains("step-1")
                        && "end".equals(edge.getTargetNodeId()));
    }

    @Test
    void mergesToolChainWithSynthesisBeforeEnd() {
        WorkflowDefinition base = ToolCallSubgraphMergerTestWorkflows.baseWorkflowWithTools();
        var mergeResult = MERGER.merge(
                base,
                ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                List.of(
                        new ParsedToolCall("olo-core:calculator", Map.of()),
                        new ParsedToolCall("olo-core:cpu-usage", Map.of())));

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
}
