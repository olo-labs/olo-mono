package org.olo.kernel.toolcall;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Removes prior runtime-injected subgraph nodes before merging a fresh expansion.
 */
public final class DynamicSubgraphStripper {

    public static final String AGENT_DYNAMIC_PREFIX = "agent-dyn-";
    public static final String TOOL_DYNAMIC_PREFIX = "tool-dyn-";

    private DynamicSubgraphStripper() {
    }

    public static WorkflowDefinition stripInjectedNodes(
            WorkflowDefinition graph, String plannerNodeId, String continueNodeId) {
        Set<String> dynamicNodeIds = graph.getNodes().stream()
                .map(NodeDefinition::getId)
                .filter(DynamicSubgraphStripper::isInjectedNodeId)
                .collect(Collectors.toSet());
        if (dynamicNodeIds.isEmpty()) {
            return graph;
        }

        List<NodeDefinition> retainedNodes = graph.getNodes().stream()
                .filter(node -> !dynamicNodeIds.contains(node.getId()))
                .toList();

        List<EdgeDefinition> retainedEdges = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (dynamicNodeIds.contains(edge.getSourceNodeId()) || dynamicNodeIds.contains(edge.getTargetNodeId())) {
                continue;
            }
            retainedEdges.add(edge);
        }

        boolean plannerToContinue = retainedEdges.stream()
                .anyMatch(edge -> plannerNodeId.equals(edge.getSourceNodeId())
                        && continueNodeId.equals(edge.getTargetNodeId()));
        if (!plannerToContinue) {
            retainedEdges.add(EdgeDefinition.builder()
                    .sourceNodeId(plannerNodeId)
                    .sourcePortId("out")
                    .targetNodeId(continueNodeId)
                    .targetPortId("in")
                    .build());
        }

        return WorkflowBuilder.from(graph).replaceNodes(retainedNodes).replaceEdges(retainedEdges).build();
    }

    public static boolean isInjectedToolNodeId(String nodeId) {
        return nodeId != null && nodeId.startsWith(TOOL_DYNAMIC_PREFIX);
    }

    public static WorkflowDefinition stripInjectedToolNodes(
            WorkflowDefinition graph, String sourceNodeId, String continueNodeId) {
        Set<String> dynamicNodeIds = graph.getNodes().stream()
                .map(NodeDefinition::getId)
                .filter(DynamicSubgraphStripper::isInjectedToolNodeId)
                .collect(Collectors.toSet());
        if (dynamicNodeIds.isEmpty()) {
            return graph;
        }

        List<NodeDefinition> retainedNodes = graph.getNodes().stream()
                .filter(node -> !dynamicNodeIds.contains(node.getId()))
                .toList();

        List<EdgeDefinition> retainedEdges = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (dynamicNodeIds.contains(edge.getSourceNodeId()) || dynamicNodeIds.contains(edge.getTargetNodeId())) {
                continue;
            }
            retainedEdges.add(edge);
        }

        boolean sourceToContinue = retainedEdges.stream()
                .anyMatch(edge -> sourceNodeId.equals(edge.getSourceNodeId())
                        && continueNodeId.equals(edge.getTargetNodeId()));
        if (!sourceToContinue) {
            retainedEdges.add(EdgeDefinition.builder()
                    .sourceNodeId(sourceNodeId)
                    .sourcePortId("out")
                    .targetNodeId(continueNodeId)
                    .targetPortId("in")
                    .build());
        }

        return WorkflowBuilder.from(graph).replaceNodes(retainedNodes).replaceEdges(retainedEdges).build();
    }

    public static boolean isInjectedNodeId(String nodeId) {
        return nodeId != null
                && (nodeId.startsWith(AGENT_DYNAMIC_PREFIX) || nodeId.startsWith(TOOL_DYNAMIC_PREFIX));
    }
}
