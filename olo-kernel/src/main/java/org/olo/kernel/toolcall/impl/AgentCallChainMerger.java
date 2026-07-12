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
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.olo.kernel.dynamicgraph.DynamicNodeLabels;
import org.olo.kernel.toolcall.AgentLabelResolver;
import org.olo.kernel.toolcall.DynamicSubgraphStripper;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.SubgraphMergeResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Injects a sequential chain of CHILD_WORKFLOW agent nodes after a planner node.
 *
 * <p>Each {@link ParsedAgentCall} becomes one dynamically labeled AGENT node that delegates to the
 * referenced child workflow. Tool nodes, when present, are chained after the last agent.
 */
public final class AgentCallChainMerger {

    private AgentCallChainMerger() {
    }

    public static SubgraphMergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            List<ParsedAgentCall> agentCalls,
            List<ParsedToolCall> toolCalls) {
        graph = DynamicSubgraphStripper.stripInjectedNodes(graph, plannerNodeId, continueNodeId);
        if (agentCalls == null || agentCalls.isEmpty()) {
            return ToolChainMerger.merge(graph, plannerNodeId, continueNodeId, toolCalls);
        }

        String prefix = "agent-dyn-" + System.nanoTime() + "-";
        List<String> dynamicAgentNodeIds = new ArrayList<>();
        WorkflowBuilder builder = WorkflowBuilder.from(graph);
        List<EdgeDefinition> mergedEdges = copyEdgesExceptPlannerBridge(graph, plannerNodeId, continueNodeId);

        for (int index = 0; index < agentCalls.size(); index++) {
            ParsedAgentCall call = agentCalls.get(index);
            String nodeId = prefix + "step-" + index;
            dynamicAgentNodeIds.add(nodeId);
            builder.addNode(buildAgentNode(graph, plannerNodeId, call, nodeId));
        }

        wireAgentChain(mergedEdges, plannerNodeId, dynamicAgentNodeIds);
        String tailNodeId = dynamicAgentNodeIds.getLast();
        if (toolCalls == null || toolCalls.isEmpty()) {
            mergedEdges.add(bridgeEdge(tailNodeId, continueNodeId));
            builder.replaceEdges(mergedEdges);
            return new SubgraphMergeResult(builder.build(), dynamicAgentNodeIds.getFirst());
        }

        builder.replaceEdges(mergedEdges);
        SubgraphMergeResult toolMerge = ToolChainMerger.merge(builder.build(), tailNodeId, continueNodeId, toolCalls);
        return new SubgraphMergeResult(toolMerge.graph(), dynamicAgentNodeIds.getFirst());
    }

    private static List<EdgeDefinition> copyEdgesExceptPlannerBridge(
            WorkflowDefinition graph, String plannerNodeId, String continueNodeId) {
        List<EdgeDefinition> mergedEdges = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (plannerNodeId.equals(edge.getSourceNodeId()) && continueNodeId.equals(edge.getTargetNodeId())) {
                continue;
            }
            mergedEdges.add(edge);
        }
        return mergedEdges;
    }

    private static NodeDefinition buildAgentNode(
            WorkflowDefinition graph, String plannerNodeId, ParsedAgentCall call, String nodeId) {
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("delegateAgentId", call.agentId());
        if (call.message() != null && !call.message().isBlank()) {
            configuration.put("delegateMessage", call.message());
        }
        return InjectedNodePortEnricher.withDefaultPorts(NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.AGENT.name())
                .label(DynamicNodeLabels.prefixedAgent(
                        AgentLabelResolver.resolve(call.agentId(), graph, plannerNodeId)))
                .executionKind(ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .workflow(WorkflowReferenceDefinition.builder()
                        .workflowId(call.agentId())
                        .version("1.0.0")
                        .build())
                .configuration(configuration)
                .build());
    }

    private static void wireAgentChain(
            List<EdgeDefinition> mergedEdges, String plannerNodeId, List<String> dynamicAgentNodeIds) {
        mergedEdges.add(bridgeEdge(plannerNodeId, dynamicAgentNodeIds.getFirst()));
        for (int index = 0; index < dynamicAgentNodeIds.size() - 1; index++) {
            mergedEdges.add(bridgeEdge(dynamicAgentNodeIds.get(index), dynamicAgentNodeIds.get(index + 1)));
        }
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
