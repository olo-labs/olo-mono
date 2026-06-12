package org.olo.kernel.traversal.step.handler.impl;

import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.core.runtime.ExecutionEngine;
import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.context.ExecutionContextFactory;
import org.olo.kernel.traversal.context.impl.VariableScopeBridge;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.request.NodeRequestFactory;
import org.olo.kernel.traversal.spi.NodeTypeResolver;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.Objects;

public final class SpiNodeTypeHandler implements NodeTypeHandler {

    private final ExecutionEngine executionEngine;
    private final ExecutionContextFactory executionContextFactory;
    private final NodeRequestFactory nodeRequestFactory;
    private final NodeTypeResolver nodeTypeResolver;

    public SpiNodeTypeHandler(
            ExecutionEngine executionEngine,
            ExecutionContextFactory executionContextFactory,
            NodeRequestFactory nodeRequestFactory,
            NodeTypeResolver nodeTypeResolver) {
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.executionContextFactory = Objects.requireNonNull(executionContextFactory, "executionContextFactory");
        this.nodeRequestFactory = Objects.requireNonNull(nodeRequestFactory, "nodeRequestFactory");
        this.nodeTypeResolver = Objects.requireNonNull(nodeTypeResolver, "nodeTypeResolver");
    }

    @Override
    public boolean supports(String nodeType) {
        String resolved = nodeTypeResolver.resolve(nodeType);
        return executionEngine.nodeRegistry().find(resolved).isPresent();
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        ExecutionContext executionContext = executionContextFactory.create(context, node.getId());
        NodeRequest request = toSpiRequest(context, node);
        TraversalDiagnostics.logNodeRequest(
                node.getId(),
                node.getType(),
                request.nodeType(),
                request.input(),
                request.configuration());
        NodeResult result = executionEngine.executeNode(request, executionContext);
        if (executionContext instanceof DefaultExecutionContext defaultContext) {
            VariableScopeBridge.copyFromExecutionContext(defaultContext, context.getVariables());
        }
        return result;
    }

    private NodeRequest toSpiRequest(KernelRuntimeContext context, NodeDefinition node) {
        NodeRequest request = nodeRequestFactory.create(context, node);
        String resolvedType = nodeTypeResolver.resolve(node.getType());
        return new NodeRequest(request.nodeId(), resolvedType, request.input(), request.configuration());
    }
}
