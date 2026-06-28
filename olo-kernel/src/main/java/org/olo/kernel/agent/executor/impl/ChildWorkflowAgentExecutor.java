package org.olo.kernel.agent.executor.impl;

import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.childworkflow.ChildWorkflowCoordinator;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.spi.node.NodeResult;

import java.util.Objects;

/**
 * Agent backed by a child workflow ({@code workflowRef} on the node).
 * Dispatches a separate workflow file and blocks until the child completes.
 */
public final class ChildWorkflowAgentExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "child-workflow";

    private final ChildWorkflowCoordinator coordinator;

    public ChildWorkflowAgentExecutor(ChildWorkflowCoordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    @Override
    public String id() {
        return EXECUTOR_ID;
    }

    @Override
    public boolean supports(NodeDefinition node) {
        if (node == null || !NodeType.AGENT.name().equals(node.getType())) {
            return false;
        }
        if (ToolCallPlannerSupport.isToolCallPlanner(node)) {
            return false;
        }
        if (Boolean.TRUE.equals(node.getConfiguration() != null
                ? node.getConfiguration().get(AgentCallDispatchExecutor.CONFIG_AGENT_CALL_DISPATCH)
                : null)) {
            return false;
        }
        if (org.olo.definition.dynamicgraph.AgentSynthesisSupport.isAgentSynthesis(node)) {
            return false;
        }
        return node.getExecutionModel() == ExecutionModel.CHILD_WORKFLOW && node.getWorkflow() != null;
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        if (!coordinator.handles(node)) {
            throw new KernelException("child workflow coordinator does not handle node: " + node.getId());
        }
        return coordinator.dispatch(context, node);
    }
}
