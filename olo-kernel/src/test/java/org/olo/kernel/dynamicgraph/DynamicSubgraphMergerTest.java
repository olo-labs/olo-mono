/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph;

import org.junit.jupiter.api.Test;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphMergeResult;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSubgraphMergerTest {

    private final DynamicSubgraphMerger merger = DynamicSubgraphFactories.defaultMerger();

    @Test
    void validatesMinimalSubgraphJson() {
        DynamicSubgraphValidationResult result = merger.validate(sampleSubgraphJson());

        assertThat(result.valid()).isTrue();
        assertThat(result.normalizedJson()).contains("\"nodes\"");
    }

    @Test
    void rejectsMarkdownWrappedJson() {
        DynamicSubgraphValidationResult result = merger.validate("```json\n" + sampleSubgraphJson() + "\n```");

        assertThat(result.valid()).isTrue();
    }

    @Test
    void mergesSubgraphIntoWorkflowAndRewiresPlannerToEnd() {
        WorkflowDefinition base = baseWorkflow();
        DynamicSubgraphMergeResult mergeResult = merger.merge(
                base,
                DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                "end",
                sampleSubgraphJson());

        assertThat(mergeResult.graph().getNodes()).hasSize(4);
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getSourceNodeId())
                        && mergeResult.entryNodeId().equals(edge.getTargetNodeId()));
        assertThat(mergeResult.graph().getEdges()).anyMatch(edge ->
                edge.getSourceNodeId() != null
                        && edge.getSourceNodeId().startsWith("dyn-")
                        && "end".equals(edge.getTargetNodeId()));
        assertThat(mergeResult.graph().getEdges()).noneMatch(edge ->
                DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getSourceNodeId())
                        && "end".equals(edge.getTargetNodeId()));
    }

    private static WorkflowDefinition baseWorkflow() {
        return WorkflowBuilder.create("Dynamic Graph Creation")
                .id("dynamic-graph-creation")
                .queue("dynamic-graph-creation")
                .startNode("start")
                .addNode(plannerNode())
                .endNode("end")
                .connect("start", "out", DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID, "in")
                .connect(DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID, "out", "end", "in")
                .build();
    }

    private static NodeDefinition plannerNode() {
        return NodeDefinition.builder()
                .id(DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID)
                .type(NodeType.AGENT.name())
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .putConfiguration(DynamicGraphPlannerSupport.CONFIG_DYNAMIC_GRAPH_PLANNER, true)
                .build();
    }

    @Test
    void rejectsInvalidNodeType() {
        DynamicSubgraphValidationResult result = merger.validate("""
                {
                  "id": "bad",
                  "label": "Bad",
                  "nodes": [
                    { "id": "start", "type": "START" },
                    { "id": "step-1", "type": "AGENT|PROMPT" },
                    { "id": "end", "type": "END" }
                  ],
                  "edges": [
                    { "sourceNodeId": "start", "targetNodeId": "step-1" },
                    { "sourceNodeId": "step-1", "targetNodeId": "end" }
                  ]
                }
                """);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("invalid node type");
    }

    @Test
    void rejectsMissingEndNode() {
        DynamicSubgraphValidationResult result = merger.validate("""
                {
                  "id": "bad",
                  "label": "Bad",
                  "nodes": [
                    { "id": "start", "type": "START" },
                    { "id": "step-1", "type": "AGENT" }
                  ],
                  "edges": [
                    { "sourceNodeId": "start", "targetNodeId": "step-1" }
                  ]
                }
                """);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("START and END");
    }

    private static String sampleSubgraphJson() {
        return """
                {
                  "id": "incident-triage",
                  "label": "Incident Triage",
                  "nodes": [
                    { "id": "start", "type": "START" },
                    { "id": "read-logs", "type": "TOOL", "configuration": { "toolId": "olo-core:log-reader" } },
                    { "id": "end", "type": "END" }
                  ],
                  "edges": [
                    {
                      "sourceNodeId": "start",
                      "sourcePortId": "out",
                      "targetNodeId": "read-logs",
                      "targetPortId": "in"
                    },
                    {
                      "sourceNodeId": "read-logs",
                      "sourcePortId": "out",
                      "targetNodeId": "end",
                      "targetPortId": "in"
                    }
                  ]
                }
                """;
    }
}
