package org.olo.kernel.childworkflow;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.toolcall.AgentCallResultsSupport;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.visit.GraphEdgeNavigator;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dispatches child workflows for {@code CHILD_WORKFLOW} agent nodes and merges results into the parent.
 */
public final class DefaultChildWorkflowCoordinator implements ChildWorkflowCoordinator {

    @Override
    public boolean handles(NodeDefinition node) {
        if (node == null) {
            return false;
        }
        String type = node.getType();
        return NodeType.AGENT.name().equals(type) || NodeType.WORKFLOW_REF.name().equals(type);
    }

    @Override
    public NodeResult dispatch(KernelRuntimeContext parent, NodeDefinition node) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(node, "node");

        String childWorkflowId = readChildWorkflowId(node);
        if (childWorkflowId == null || childWorkflowId.isBlank()) {
            throw new KernelException("child workflow id is required for node: " + node.getId());
        }
        if (ChildWorkflowDelegationSupport.isSelfReference(parent, node)) {
            throw new KernelException(
                    "recursive child workflow delegation is not allowed for node: "
                            + node.getId()
                            + " workflowId="
                            + childWorkflowId);
        }

        String childMessage = readDelegateMessage(node, parent);
        var childInput = ChildWorkflowInputs.forChildAgent(parent, childWorkflowId, childMessage);
        String childResult = ChildWorkflowRunGateway.execute(parent.getQueue(), childWorkflowId, childInput);

        NodeResult childNodeResult = NodeResult.completed(childResult, Map.of("response", childResult));
        mergeOutputs(parent, node, childNodeResult);
        AgentCallResultsSupport.appendResult(
                parent.getVariables(), childWorkflowId, childMessage, childResult);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("response", childResult);
        output.put("childWorkflowId", childWorkflowId);
        output.put("agentExecutor", "child-workflow");
        return NodeResult.completed(childResult, output);
    }

    @Override
    public NodeResult resume(KernelRuntimeContext parent, NodeDefinition node, ChildWorkflowResumeSignal signal) {
        throw new KernelException("child workflow resume is not implemented for synchronous child dispatch");
    }

    @Override
    public void mergeOutputs(KernelRuntimeContext parent, NodeDefinition node, NodeResult childResult) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(node, "node");
        if (childResult == null || childResult.status() != NodeStatus.COMPLETED) {
            return;
        }
        Object response = childResult.output() != null ? childResult.output().get("response") : null;
        String message = childResult.message();
        if (response == null && (message == null || message.isBlank())) {
            return;
        }
        String childWorkflowId = readChildWorkflowId(node);
        parent.getVariables().set("childWorkflowResult:" + childWorkflowId, response != null ? response : message);
    }

    @Override
    public Optional<String> nextNodeId(
            KernelRuntimeContext parent, GraphIndex graphIndex, NodeDefinition node, NodeResult stepResult) {
        return GraphEdgeNavigator.firstTarget(graphIndex, node.getId());
    }

    private static String readChildWorkflowId(NodeDefinition node) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateAgentId");
            if (configured != null) {
                String delegateAgentId = String.valueOf(configured).trim();
                if (!delegateAgentId.isBlank()) {
                    return delegateAgentId;
                }
            }
        }
        if (node.getWorkflow() != null && node.getWorkflow().getWorkflowId() != null) {
            return node.getWorkflow().getWorkflowId().trim();
        }
        return null;
    }

    private static String readDelegateMessage(NodeDefinition node, KernelRuntimeContext parent) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateMessage");
            if (configured != null) {
                String message = String.valueOf(configured).trim();
                if (!message.isBlank()) {
                    return message;
                }
            }
        }
        return null;
    }
}
