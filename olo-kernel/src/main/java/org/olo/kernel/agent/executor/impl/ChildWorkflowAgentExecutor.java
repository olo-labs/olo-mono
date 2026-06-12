package org.olo.kernel.agent.executor.impl;

import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.spi.node.NodeResult;

/**
 * Agent backed by a child workflow ({@code workflowRef} on the node).
 * <p>
 * When enabled, delegates dispatch / wait / resume / output merge to
 * {@link org.olo.kernel.childworkflow.ChildWorkflowCoordinator}. Not wired yet — {@link #supports}
 * returns {@code false} so existing presets keep using {@link LocalLlmAgentExecutor}.
 */
public final class ChildWorkflowAgentExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "child-workflow";

    /** Set true when kernel dispatches {@code workflowRef} child workflows. */
    private static final boolean DISPATCH_ENABLED = false;

    @Override
    public String id() {
        return EXECUTOR_ID;
    }

    @Override
    public boolean supports(NodeDefinition node) {
        if (node == null || !NodeType.AGENT.name().equals(node.getType())) {
            return false;
        }
        return DISPATCH_ENABLED
                && node.getExecutionModel() == ExecutionModel.CHILD_WORKFLOW
                && node.getWorkflow() != null;
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        throw new KernelException(
                "ChildWorkflowAgentExecutor is not implemented for node: " + node.getId()
                        + "; enable supports() when child workflow dispatch is wired");
    }
}
