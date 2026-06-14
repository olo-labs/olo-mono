package org.olo.kernel.traversal.strategy;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.spi.node.NodeResult;

import java.util.Objects;

/**
 * Input to an {@link ExecutionStrategy} after a node has been executed.
 */
public record ExecutionStrategyRequest(
        KernelRuntimeContext context,
        GraphIndex graphIndex,
        MutableGraphSession graphSession,
        NodeDefinition completedNode,
        NodeResult nodeResult,
        int step) {

    public ExecutionStrategyRequest {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(graphIndex, "graphIndex");
        Objects.requireNonNull(completedNode, "completedNode");
        Objects.requireNonNull(nodeResult, "nodeResult");
    }

    public ExecutionStrategyRequest(
            KernelRuntimeContext context,
            GraphIndex graphIndex,
            NodeDefinition completedNode,
            NodeResult nodeResult,
            int step) {
        this(context, graphIndex, null, completedNode, nodeResult, step);
    }
}
