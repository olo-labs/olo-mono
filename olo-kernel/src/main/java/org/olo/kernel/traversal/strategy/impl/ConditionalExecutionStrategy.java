package org.olo.kernel.traversal.strategy.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeRouterDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategy;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeResult;

import java.util.Map;
import java.util.Optional;

/**
 * Routes to a target node or port for {@link NodeType#CONDITION} and {@link NodeType#ROUTER} nodes.
 */
public final class ConditionalExecutionStrategy implements ExecutionStrategy {

    public static final String STRATEGY_NAME = "conditional";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean supports(ExecutionStrategyRequest request) {
        String type = request.completedNode().getType();
        return NodeType.CONDITION.name().equals(type) || NodeType.ROUTER.name().equals(type);
    }

    @Override
    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        NodeDefinition node = request.completedNode();
        NodeResult result = request.nodeResult();

        Optional<String> fromOutput = selectedPortFromResult(result)
                .flatMap(port -> GraphEdgeNavigator.targetBySourcePort(request.graphIndex(), node.getId(), port));
        if (fromOutput.isPresent()) {
            return ExecutionDecision.linear(STRATEGY_NAME, fromOutput.get());
        }

        Optional<String> fromRouter = routerTarget(request.graphIndex(), node, result);
        if (fromRouter.isPresent()) {
            return ExecutionDecision.linear(STRATEGY_NAME, fromRouter.get());
        }

        return GraphEdgeNavigator.firstTarget(request.graphIndex(), node.getId())
                .map(next -> ExecutionDecision.linear(STRATEGY_NAME, next))
                .orElseGet(() -> ExecutionDecision.end(STRATEGY_NAME));
    }

    private static Optional<String> selectedPortFromResult(NodeResult result) {
        if (result == null || result.output() == null || result.output().isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> output = result.output();
        for (String key : new String[] {"selectedPort", "route", "port", "branch"}) {
            Object value = output.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return Optional.of(text.trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> routerTarget(
            org.olo.kernel.graph.index.GraphIndex graphIndex, NodeDefinition node, NodeResult result) {
        for (NodeRouterDefinition router : node.getRouters()) {
            if (router == null) {
                continue;
            }
            String targetNodeId = router.getTargetNodeId();
            if (targetNodeId != null && !targetNodeId.isBlank()) {
                return Optional.of(targetNodeId.trim());
            }
            String targetPort = router.getTargetPort();
            if (targetPort != null && !targetPort.isBlank()) {
                Optional<String> byPort =
                        GraphEdgeNavigator.targetBySourcePort(graphIndex, node.getId(), targetPort);
                if (byPort.isPresent()) {
                    return byPort;
                }
            }
        }
        return Optional.empty();
    }
}
