package org.olo.kernel.traversal.step.handler.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.input.WorkflowInputBinder;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.spi.node.NodeResult;

import java.util.Map;

public final class StartNodeTypeHandler implements NodeTypeHandler {

    private final WorkflowInputBinder inputBinder;

    public StartNodeTypeHandler(WorkflowInputBinder inputBinder) {
        this.inputBinder = inputBinder;
    }

    @Override
    public boolean supports(String nodeType) {
        return NodeType.START.name().equals(nodeType);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        inputBinder.bind(context);
        return NodeResult.completed(Map.of());
    }
}
