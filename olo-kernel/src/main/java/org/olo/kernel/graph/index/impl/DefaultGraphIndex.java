package org.olo.kernel.graph.index.impl;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.graph.index.GraphIndex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultGraphIndex implements GraphIndex {

    private final Map<String, NodeDefinition> nodesById;
    private final Map<String, List<EdgeDefinition>> outgoingByNodeId;

    public DefaultGraphIndex(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        this.nodesById = indexNodes(graph.getNodes());
        this.outgoingByNodeId = indexOutgoingEdges(graph.getEdges());
    }

    @Override
    public Optional<NodeDefinition> findNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    @Override
    public List<EdgeDefinition> outgoingEdges(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return List.of();
        }
        return outgoingByNodeId.getOrDefault(nodeId, List.of());
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.copyOf(nodesById.values());
    }

    private static Map<String, NodeDefinition> indexNodes(List<NodeDefinition> nodes) {
        Map<String, NodeDefinition> indexed = new LinkedHashMap<>();
        if (nodes == null) {
            return indexed;
        }
        for (NodeDefinition node : nodes) {
            if (node != null && node.getId() != null && !node.getId().isBlank()) {
                indexed.put(node.getId(), node);
            }
        }
        return indexed;
    }

    private static Map<String, List<EdgeDefinition>> indexOutgoingEdges(List<EdgeDefinition> edges) {
        Map<String, List<EdgeDefinition>> indexed = new LinkedHashMap<>();
        if (edges == null) {
            return indexed;
        }
        for (EdgeDefinition edge : edges) {
            if (edge == null || edge.getSourceNodeId() == null || edge.getSourceNodeId().isBlank()) {
                continue;
            }
            indexed.computeIfAbsent(edge.getSourceNodeId(), ignored -> new ArrayList<>()).add(edge);
        }
        for (List<EdgeDefinition> outgoing : indexed.values()) {
            outgoing.sort((left, right) -> compareEdge(left, right));
        }
        return indexed;
    }

    private static int compareEdge(EdgeDefinition left, EdgeDefinition right) {
        String leftTarget = left.getTargetNodeId() != null ? left.getTargetNodeId() : "";
        String rightTarget = right.getTargetNodeId() != null ? right.getTargetNodeId() : "";
        return leftTarget.compareTo(rightTarget);
    }
}
