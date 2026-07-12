/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.dynamicgraph.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.dynamicgraph.DynamicNodeLabels;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphMergeResult;
import org.olo.kernel.toolcall.impl.InjectedNodePortEnricher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splices a validated dynamic subgraph into the parent workflow graph.
 */
final class DynamicSubgraphGraphMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DynamicSubgraphGraphMerger() {
    }

    static DynamicSubgraphMergeResult merge(
            WorkflowDefinition graph,
            String plannerNodeId,
            String continueNodeId,
            String validatedJson) {
        try {
            JsonNode root = MAPPER.readTree(validatedJson);
            List<NodeDefinition> subgraphNodes = DynamicSubgraphJsonParser.parseNodes(root.get("nodes"));
            List<EdgeDefinition> subgraphEdges = DynamicSubgraphJsonParser.parseEdges(root.get("edges"));

            String startId = subgraphNodes.stream()
                    .filter(node -> NodeType.START.name().equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElseThrow();
            String endId = subgraphNodes.stream()
                    .filter(node -> NodeType.END.name().equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElseThrow();

            String entryNodeId = subgraphEdges.stream()
                    .filter(edge -> startId.equals(edge.getSourceNodeId()))
                    .map(EdgeDefinition::getTargetNodeId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("subgraph START has no outgoing edge"));

            List<String> exitNodeIds = subgraphEdges.stream()
                    .filter(edge -> endId.equals(edge.getTargetNodeId()))
                    .map(EdgeDefinition::getSourceNodeId)
                    .distinct()
                    .toList();
            if (exitNodeIds.isEmpty()) {
                throw new IllegalStateException("subgraph END has no incoming edge");
            }

            // Prefix injected node ids so they cannot collide with the parent graph.
            String prefix = "dyn-" + System.nanoTime() + "-";
            Map<String, String> idMap = new LinkedHashMap<>();
            for (NodeDefinition node : subgraphNodes) {
                if (NodeType.START.name().equals(node.getType()) || NodeType.END.name().equals(node.getType())) {
                    continue;
                }
                idMap.put(node.getId(), prefix + node.getId());
            }

            WorkflowBuilder builder = WorkflowBuilder.from(graph);
            List<EdgeDefinition> mergedEdges = new ArrayList<>();
            for (EdgeDefinition edge : graph.getEdges()) {
                if (plannerNodeId.equals(edge.getSourceNodeId()) && continueNodeId.equals(edge.getTargetNodeId())) {
                    continue;
                }
                mergedEdges.add(edge);
            }

            for (NodeDefinition node : subgraphNodes) {
                if (NodeType.START.name().equals(node.getType()) || NodeType.END.name().equals(node.getType())) {
                    continue;
                }
                NodeDefinition remapped = InjectedNodePortEnricher.withDefaultPorts(
                        DynamicNodeLabels.withDynamicLabel(remapNode(node, idMap.get(node.getId())), graph));
                builder.addNode(remapped);
            }

            for (EdgeDefinition edge : subgraphEdges) {
                if (startId.equals(edge.getSourceNodeId()) || endId.equals(edge.getTargetNodeId())) {
                    continue;
                }
                mergedEdges.add(remapEdge(edge, idMap));
            }

            String prefixedEntry = idMap.get(entryNodeId);
            mergedEdges.add(EdgeDefinition.builder()
                    .sourceNodeId(plannerNodeId)
                    .sourcePortId("out")
                    .targetNodeId(prefixedEntry)
                    .targetPortId("in")
                    .build());
            for (String exitNodeId : exitNodeIds) {
                mergedEdges.add(EdgeDefinition.builder()
                        .sourceNodeId(idMap.get(exitNodeId))
                        .sourcePortId("out")
                        .targetNodeId(continueNodeId)
                        .targetPortId("in")
                        .build());
            }

            builder.replaceEdges(mergedEdges);
            return new DynamicSubgraphMergeResult(builder.build(), prefixedEntry);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("failed to merge generated subgraph: " + e.getMessage(), e);
        }
    }

    private static NodeDefinition remapNode(NodeDefinition node, String newId) {
        NodeDefinitionBuilder builder = NodeDefinition.builder()
                .id(newId)
                .type(node.getType())
                .configuration(node.getConfiguration());
        if (node.getLabel() != null && !node.getLabel().isBlank()) {
            builder.label(node.getLabel());
        }
        return builder.build();
    }

    private static EdgeDefinition remapEdge(EdgeDefinition edge, Map<String, String> idMap) {
        return EdgeDefinition.builder()
                .sourceNodeId(idMap.get(edge.getSourceNodeId()))
                .sourcePortId(edge.getSourcePortId())
                .targetNodeId(idMap.get(edge.getTargetNodeId()))
                .targetPortId(edge.getTargetPortId())
                .build();
    }
}
