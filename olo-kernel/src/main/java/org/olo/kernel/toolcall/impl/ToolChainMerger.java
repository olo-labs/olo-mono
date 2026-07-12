/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.impl;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.dynamicgraph.ToolSynthesisSupport;
import org.olo.kernel.dynamicgraph.DynamicNodeLabels;
import org.olo.kernel.toolcall.DynamicSubgraphStripper;
import org.olo.kernel.toolcall.ToolLabelResolver;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.SubgraphMergeResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Injects TOOL nodes plus an inline synthesis AGENT between a source node and continue node.
 */
public final class ToolChainMerger {

    private ToolChainMerger() {
    }

    public static SubgraphMergeResult merge(
            WorkflowDefinition graph,
            String sourceNodeId,
            String continueNodeId,
            List<ParsedToolCall> toolCalls) {
        graph = DynamicSubgraphStripper.stripInjectedToolNodes(graph, sourceNodeId, continueNodeId);
        String prefix = "tool-dyn-" + System.nanoTime() + "-";
        List<String> dynamicNodeIds = new ArrayList<>();
        WorkflowBuilder builder = WorkflowBuilder.from(graph);
        List<EdgeDefinition> mergedEdges = copyEdgesExceptBridge(graph, sourceNodeId, continueNodeId);

        for (int index = 0; index < toolCalls.size(); index++) {
            ParsedToolCall call = toolCalls.get(index);
            String nodeId = prefix + "step-" + index;
            dynamicNodeIds.add(nodeId);
            builder.addNode(buildToolNode(graph, call, nodeId));
        }

        String synthesisNodeId = prefix + ToolCallPlannerSupport.DEFAULT_SYNTHESIS_NODE_SUFFIX;
        builder.addNode(buildSynthesisNode(synthesisNodeId));
        wireToolChain(mergedEdges, sourceNodeId, continueNodeId, dynamicNodeIds, synthesisNodeId);

        builder.replaceEdges(mergedEdges);
        return new SubgraphMergeResult(builder.build(), dynamicNodeIds.getFirst());
    }

    private static List<EdgeDefinition> copyEdgesExceptBridge(
            WorkflowDefinition graph, String sourceNodeId, String continueNodeId) {
        List<EdgeDefinition> mergedEdges = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (sourceNodeId.equals(edge.getSourceNodeId()) && continueNodeId.equals(edge.getTargetNodeId())) {
                continue;
            }
            mergedEdges.add(edge);
        }
        return mergedEdges;
    }

    private static NodeDefinition buildToolNode(WorkflowDefinition graph, ParsedToolCall call, String nodeId) {
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("toolId", call.toolId());
        if (!call.arguments().isEmpty()) {
            configuration.put("arguments", call.arguments());
        }
        configuration.put(ToolCallPlannerSupport.CONFIG_AGGREGATE_TOOL_RESULT, true);
        return InjectedNodePortEnricher.withDefaultPorts(NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.TOOL.name())
                .label(DynamicNodeLabels.prefixedTool(ToolLabelResolver.resolve(call.toolId(), graph)))
                .configuration(configuration)
                .build());
    }

    private static NodeDefinition buildSynthesisNode(String synthesisNodeId) {
        return InjectedNodePortEnricher.withDefaultPorts(NodeDefinition.builder()
                .id(synthesisNodeId)
                .type(NodeType.AGENT.name())
                .label(DynamicNodeLabels.prefixedAgent("Tool synthesis"))
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .putConfiguration("promptTemplate", ToolCallPlannerSupport.TOOL_SYNTHESIS_PROMPT_TEMPLATE)
                .putConfiguration(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS, true)
                .build());
    }

    private static void wireToolChain(
            List<EdgeDefinition> mergedEdges,
            String sourceNodeId,
            String continueNodeId,
            List<String> dynamicNodeIds,
            String synthesisNodeId) {
        mergedEdges.add(bridgeEdge(sourceNodeId, dynamicNodeIds.getFirst()));
        for (int index = 0; index < dynamicNodeIds.size(); index++) {
            String current = dynamicNodeIds.get(index);
            String next = index + 1 < dynamicNodeIds.size() ? dynamicNodeIds.get(index + 1) : synthesisNodeId;
            mergedEdges.add(bridgeEdge(current, next));
        }
        mergedEdges.add(bridgeEdge(synthesisNodeId, continueNodeId));
    }

    private static EdgeDefinition bridgeEdge(String sourceNodeId, String targetNodeId) {
        return EdgeDefinition.builder()
                .sourceNodeId(sourceNodeId)
                .sourcePortId("out")
                .targetNodeId(targetNodeId)
                .targetPortId("in")
                .build();
    }
}
