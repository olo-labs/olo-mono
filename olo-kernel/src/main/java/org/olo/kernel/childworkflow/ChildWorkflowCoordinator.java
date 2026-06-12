package org.olo.kernel.childworkflow;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.spi.node.NodeResult;

import java.util.Optional;

/**
 * Owns the child-workflow lifecycle for {@code AGENT} and {@code WORKFLOW_REF} nodes with
 * {@link org.olo.definition.execution.ExecutionModel#CHILD_WORKFLOW}.
 * <p>
 * Dispatch, parent suspension ({@code WAITING}), resume, output merge, and post-child navigation
 * must not be split across {@link org.olo.kernel.agent.executor.AgentExecutor} and
 * {@link org.olo.kernel.traversal.strategy.impl.ChildWorkflowExecutionStrategy} — both delegate here.
 * The execution strategy only decides that navigation for this step goes through the coordinator.
 */
public interface ChildWorkflowCoordinator {

    /** Whether this coordinator handles the given node (execution model + type + refs). */
    boolean handles(NodeDefinition node);

    /**
     * Start or continue child execution. May return {@code WAITING} while the child runs.
     */
    NodeResult dispatch(KernelRuntimeContext parent, NodeDefinition node);

    /**
     * Resume the parent step after a child completion or external signal.
     */
    NodeResult resume(KernelRuntimeContext parent, NodeDefinition node, ChildWorkflowResumeSignal signal);

    /**
     * Merge child outputs into the parent context ({@code ExecutionOutputs}, AGENT-scope variables).
     */
    void mergeOutputs(KernelRuntimeContext parent, NodeDefinition node, NodeResult childResult);

    /**
     * Next graph node after the child-workflow step is settled.
     * Called from {@link org.olo.kernel.traversal.strategy.impl.ChildWorkflowExecutionStrategy};
     * the strategy must not compute successor edges independently.
     */
    Optional<String> nextNodeId(
            KernelRuntimeContext parent,
            GraphIndex graphIndex,
            NodeDefinition node,
            NodeResult stepResult);
}
