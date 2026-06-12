package org.olo.kernel.traversal.step.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.output.NodeOutputApplier;
import org.olo.kernel.traversal.step.TraversalStepExecutor;
import org.olo.kernel.traversal.step.handler.impl.NodeTypeHandlerRegistry;
import org.olo.spi.node.NodeResult;

import java.util.Objects;

public final class DefaultTraversalStepExecutor implements TraversalStepExecutor {

    private final NodeTypeHandlerRegistry handlerRegistry;
    private final NodeOutputApplier outputApplier;

    public DefaultTraversalStepExecutor(
            NodeTypeHandlerRegistry handlerRegistry, NodeOutputApplier outputApplier) {
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
        this.outputApplier = Objects.requireNonNull(outputApplier, "outputApplier");
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        return execute(context, node, 0);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node, int step) {
        NodeResult result = handlerRegistry.resolve(node.getType()).execute(context, node);
        TraversalDiagnostics.logNodeResult(node.getId(), node.getType(), result);
        outputApplier.apply(context, node, result);
        return result;
    }
}
