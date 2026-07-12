/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;

import java.util.List;

/**
 * Validates execution model and kind on AGENT nodes.
 *
 * <p>Most agent nodes run as child workflows ({@code CHILD_WORKFLOW} / {@code SUBWORKFLOW}).
 * Special planner presets and leaf self-references run inline ({@code INLINE} / {@code ACTIVITY})
 * because they execute locally within the parent workflow rather than spawning a separate run.
 */
final class WorkflowAgentExecutionValidator {

    private WorkflowAgentExecutionValidator() {
    }

    /** Applies agent execution rules when the node type is AGENT. */
    static void validate(String workflowId, NodeDefinition node, List<String> errors) {
        if (!NodeType.AGENT.value().equals(node.getType())) {
            return;
        }
        String nodeId = node.getId();
        if (org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)) {
            requireInlineActivity(nodeId, "dynamic graph planner AGENT node", node, errors);
            return;
        }
        if (org.olo.definition.toolcall.ToolCallPlannerSupport.isToolCallPlanner(node)) {
            requireInlineActivity(nodeId, "tool-call planner AGENT node", node, errors);
            return;
        }
        if (org.olo.definition.dynamicgraph.DynamicSubgraphInjectionSupport.isToolSynthesis(node)) {
            requireInlineActivity(nodeId, "tool synthesis AGENT node", node, errors);
            return;
        }
        if (isLeafSelfAgent(workflowId, node)) {
            requireInlineActivity(nodeId, "leaf self AGENT node", node, errors);
            return;
        }
        if (node.getExecutionModel() != ExecutionModel.CHILD_WORKFLOW) {
            errors.add("AGENT node " + nodeId + " requires execution.executionModel CHILD_WORKFLOW");
        }
        if (node.getExecutionKind() != ExecutionKind.SUBWORKFLOW) {
            errors.add("AGENT node " + nodeId + " requires execution.executionKind SUBWORKFLOW");
        }
    }

    /**
     * Leaf agent preset: workflowRef points at the same workflow file; runs inline (local LLM),
     * not as a child run.
     */
    static boolean isLeafSelfAgent(String workflowId, NodeDefinition node) {
        if (ValidationUtils.isBlank(workflowId) || node.getWorkflow() == null) {
            return false;
        }
        String refId = node.getWorkflow().getWorkflowId();
        return !ValidationUtils.isBlank(refId) && workflowId.trim().equals(refId.trim());
    }

    private static void requireInlineActivity(
            String nodeId, String label, NodeDefinition node, List<String> errors) {
        if (node.getExecutionModel() != ExecutionModel.INLINE) {
            errors.add(label + " " + nodeId + " requires execution.executionModel INLINE");
        }
        if (node.getExecutionKind() != ExecutionKind.ACTIVITY) {
            errors.add(label + " " + nodeId + " requires execution.executionKind ACTIVITY");
        }
    }
}
