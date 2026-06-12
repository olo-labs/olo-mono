package org.olo.kernel.graph.visit;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.kernel.graph.index.GraphIndex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Shared edge navigation helpers for execution strategies.
 */
public final class GraphEdgeNavigator {

    private GraphEdgeNavigator() {
    }

    public static List<EdgeDefinition> outgoing(GraphIndex index, String nodeId) {
        if (index == null || nodeId == null || nodeId.isBlank()) {
            return List.of();
        }
        return index.outgoingEdges(nodeId);
    }

    public static Optional<String> firstTarget(GraphIndex index, String nodeId) {
        List<EdgeDefinition> outgoing = outgoing(index, nodeId);
        if (outgoing.isEmpty()) {
            return Optional.empty();
        }
        String targetNodeId = outgoing.getFirst().getTargetNodeId();
        if (targetNodeId == null || targetNodeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(targetNodeId);
    }

    public static Optional<String> targetBySourcePort(GraphIndex index, String nodeId, String sourcePortId) {
        if (sourcePortId == null || sourcePortId.isBlank()) {
            return Optional.empty();
        }
        for (EdgeDefinition edge : outgoing(index, nodeId)) {
            if (sourcePortId.equals(edge.getSourcePortId())) {
                String targetNodeId = edge.getTargetNodeId();
                if (targetNodeId != null && !targetNodeId.isBlank()) {
                    return Optional.of(targetNodeId);
                }
            }
        }
        return Optional.empty();
    }

    public static List<String> allTargets(GraphIndex index, String nodeId) {
        List<String> targets = new ArrayList<>();
        for (EdgeDefinition edge : outgoing(index, nodeId)) {
            String targetNodeId = edge.getTargetNodeId();
            if (targetNodeId != null && !targetNodeId.isBlank()) {
                targets.add(targetNodeId);
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Closest node reachable from every branch (parallel join point).
     */
    public static Optional<String> findCommonJoinNode(GraphIndex index, List<String> branchEntryNodeIds) {
        if (index == null || branchEntryNodeIds == null || branchEntryNodeIds.isEmpty()) {
            return Optional.empty();
        }
        List<Map<String, Integer>> depthMaps = new ArrayList<>();
        for (String branchStart : branchEntryNodeIds) {
            depthMaps.add(shortestDepthsFrom(index, branchStart));
        }
        Set<String> common = new HashSet<>(depthMaps.getFirst().keySet());
        for (int i = 1; i < depthMaps.size(); i++) {
            common.retainAll(depthMaps.get(i).keySet());
        }
        if (common.isEmpty()) {
            return Optional.empty();
        }
        Comparator<String> byClosestJoin = Comparator.<String>comparingInt(
                        node -> maxDepthAcrossBranches(node, depthMaps))
                .thenComparing(node -> node);
        return common.stream().min(byClosestJoin);
    }

    private static int maxDepthAcrossBranches(String nodeId, List<Map<String, Integer>> depthMaps) {
        int max = 0;
        for (Map<String, Integer> depths : depthMaps) {
            max = Math.max(max, depths.getOrDefault(nodeId, Integer.MAX_VALUE));
        }
        return max;
    }

    private static Map<String, Integer> shortestDepthsFrom(GraphIndex index, String startNodeId) {
        Map<String, Integer> depths = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(startNodeId);
        depths.put(startNodeId, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depths.get(current);
            for (EdgeDefinition edge : outgoing(index, current)) {
                String target = edge.getTargetNodeId();
                if (target == null || target.isBlank() || depths.containsKey(target)) {
                    continue;
                }
                depths.put(target, currentDepth + 1);
                queue.add(target);
            }
        }
        depths.remove(startNodeId);
        return depths;
    }
}
