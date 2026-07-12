/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.parallel.JoinDefinition;
import org.olo.definition.parallel.JoinStrategy;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.List;
import java.util.Set;

/**
 * Validates node-level semantic rules: approval, workflow refs, joins, execution mapping,
 * failure handling, and conditional routers.
 */
final class WorkflowNodeValidator {

    private WorkflowNodeValidator() {
    }

    /**
     * Second-pass node validation that depends on the full set of known node ids and
     * declared field names.
     */
    static void validateNodeSemantics(
            String workflowId,
            NodeDefinition node,
            Set<String> nodeIds,
            WorkflowValidationState state,
            List<String> errors) {
        WorkflowNodeFailureValidator.validateNodeRouters(node, nodeIds, errors);
        WorkflowNodeFailureValidator.validateOnFailure(node, nodeIds, errors);
        validateHumanApproval(node, errors);
        validateExecutionMapping(node, errors);
        WorkflowAgentExecutionValidator.validate(workflowId, node, errors);
        validateWorkflowReference(node, errors);
        validateParallelJoin(node, errors);
        WorkflowDataPathValidator.validateNode(
                node,
                state.stateFieldNames,
                state.inputFieldNames,
                state.parameterFieldNames,
                errors);
    }

    /** HUMAN nodes require an approval block; approval is forbidden on other node types. */
    private static void validateHumanApproval(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        boolean isHuman = NodeType.HUMAN.value().equals(node.getType());
        HumanApprovalDefinition approval = node.getApproval();
        if (approval != null && !isHuman) {
            errors.add("approval is only valid on HUMAN nodes, found on: " + nodeId);
            return;
        }
        if (!isHuman) {
            return;
        }
        if (approval == null) {
            errors.add("HUMAN node " + nodeId + " requires an approval block");
            return;
        }
        if (ValidationUtils.isBlank(approval.getTitle())) {
            errors.add("approval title is required on HUMAN node: " + nodeId);
        }
        if (approval.getApprovers().isEmpty()) {
            errors.add("approval approvers must not be empty on HUMAN node: " + nodeId);
        }
        if (approval.getTimeoutSeconds() != null && approval.getTimeoutSeconds() < 0) {
            errors.add("approval timeoutSeconds must be >= 0 on HUMAN node: " + nodeId);
        }
    }

    /** Ensures executionKind and executionModel are consistent with the node type. */
    private static void validateExecutionMapping(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        String type = node.getType();
        ExecutionKind kind = node.getExecutionKind();
        ExecutionModel model = node.getExecutionModel();
        if (kind == ExecutionKind.HUMAN_WAIT && !NodeType.HUMAN.value().equals(type)) {
            errors.add("executionKind HUMAN_WAIT requires type HUMAN on node: " + nodeId);
        }
        if (NodeType.HUMAN.value().equals(type)
                && kind != null
                && kind != ExecutionKind.HUMAN_WAIT
                && kind != ExecutionKind.EVENT) {
            errors.add("executionKind on HUMAN node " + nodeId + " should be HUMAN_WAIT or EVENT");
        }
        if (model != null && kind != null && model.expectedExecutionKind() != kind) {
            errors.add(
                    "executionModel "
                            + model
                            + " requires executionKind "
                            + model.expectedExecutionKind()
                            + " on node: "
                            + nodeId);
        }
    }

    /** Validates workflow reference presence and placement on AGENT and WORKFLOW_REF nodes. */
    private static void validateWorkflowReference(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        String type = node.getType();
        WorkflowReferenceDefinition workflow = node.getWorkflow();

        if (NodeType.AGENT.value().equals(type)
                && workflow == null
                && !org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)
                && !org.olo.definition.toolcall.ToolCallPlannerSupport.isToolCallPlanner(node)
                && !org.olo.definition.dynamicgraph.DynamicSubgraphInjectionSupport.isToolSynthesis(node)) {
            errors.add("AGENT node " + nodeId + " requires a workflow reference (agent = workflow)");
        }
        if (NodeType.WORKFLOW_REF.value().equals(type) && workflow == null) {
            errors.add("WORKFLOW_REF node " + nodeId + " requires a workflow reference");
        }
        if (workflow != null
                && !NodeType.WORKFLOW_REF.value().equals(type)
                && !NodeType.AGENT.value().equals(type)) {
            errors.add("workflow is only valid on AGENT or WORKFLOW_REF nodes, found on: " + nodeId);
        }
        if ("SUBGRAPH".equals(type) || "SUBWORKFLOW".equals(type)) {
            errors.add(
                    "type "
                            + type
                            + " on node "
                            + nodeId
                            + " is not supported; use WORKFLOW_REF with workflow instead");
        }
    }

    /** PARALLEL nodes require a join definition with a valid strategy and quorum count. */
    private static void validateParallelJoin(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        JoinDefinition join = node.getJoin();
        if (NodeType.PARALLEL.value().equals(node.getType())) {
            if (join == null) {
                errors.add("PARALLEL node " + nodeId + " requires a join definition");
                return;
            }
            if (join.getStrategy() == null) {
                errors.add("join strategy is required on PARALLEL node: " + nodeId);
            }
            if (join.getStrategy() == JoinStrategy.QUORUM
                    && (join.getQuorumCount() == null || join.getQuorumCount() < 1)) {
                errors.add("join quorumCount must be >= 1 when strategy is QUORUM on node: " + nodeId);
            }
        } else if (join != null) {
            errors.add("join is only valid on PARALLEL nodes, found on: " + nodeId);
        }
    }
}
