/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.childworkflow;

import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Guards against recursive child-workflow delegation.
 */
public final class ChildWorkflowDelegationSupport {

    private ChildWorkflowDelegationSupport() {
    }

    /**
     * Returns {@code true} when the node would dispatch to the workflow currently being executed.
     * Self-referencing AGENT nodes run inline (local LLM) instead of spawning another child run.
     */
    public static boolean isSelfReference(KernelRuntimeContext context, NodeDefinition node) {
        if (context == null || context.getGraph() == null || node == null) {
            return false;
        }
        String activeWorkflowId = context.getGraph().getId();
        if (activeWorkflowId == null || activeWorkflowId.isBlank()) {
            return false;
        }
        String targetWorkflowId = readTargetWorkflowId(node);
        return activeWorkflowId.equals(targetWorkflowId);
    }

    /**
     * Returns {@code true} when the node should dispatch to a different workflow file.
     */
    public static boolean isExternalChildTarget(KernelRuntimeContext context, NodeDefinition node) {
        if (node == null || !NodeType.AGENT.name().equals(node.getType())) {
            return false;
        }
        if (node.getExecutionModel() != ExecutionModel.CHILD_WORKFLOW) {
            return false;
        }
        WorkflowReferenceDefinition workflow = node.getWorkflow();
        if (workflow == null || workflow.getWorkflowId() == null || workflow.getWorkflowId().isBlank()) {
            return false;
        }
        return !isSelfReference(context, node);
    }

    public static String readTargetWorkflowId(NodeDefinition node) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateAgentId");
            if (configured != null) {
                String delegateAgentId = String.valueOf(configured).trim();
                if (!delegateAgentId.isBlank()) {
                    return delegateAgentId;
                }
            }
        }
        WorkflowReferenceDefinition workflow = node.getWorkflow();
        if (workflow != null && workflow.getWorkflowId() != null) {
            return workflow.getWorkflowId().trim();
        }
        return null;
    }
}
