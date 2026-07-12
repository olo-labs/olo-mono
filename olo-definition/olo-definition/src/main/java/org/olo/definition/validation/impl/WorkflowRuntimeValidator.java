/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.RuntimeBindingValidator;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/**
 * Validates runtime contracts and runtime bindings on workflows, nodes, tools, and agents.
 *
 * <p>Runtime definitions describe how execution is hosted (contract version, execution model).
 * Runtime bindings tie registry entries or nodes to concrete runtime implementations.
 */
final class WorkflowRuntimeValidator {

    private WorkflowRuntimeValidator() {
    }

    /** Validates the workflow-level runtime block and top-level runtime binding. */
    static void validateWorkflow(WorkflowDefinition workflow, List<String> errors) {
        validateWorkflowRuntimeContract("workflow " + workflow.getId(), workflow.getRuntime(), errors);
        RuntimeBindingValidator.validate(
                "workflow " + workflow.getId(), workflow.getRuntimeBinding(), errors);
    }

    /** Validates an optional per-node runtime binding (only allowed on certain node types). */
    static void validateNodeBinding(NodeDefinition node, List<String> errors) {
        if (node.getRuntimeBinding() == null) {
            return;
        }
        String type = node.getType();
        boolean allowed =
                NodeType.AGENT.value().equals(type)
                        || NodeType.WORKFLOW_REF.value().equals(type)
                        || NodeType.TOOL.value().equals(type);
        if (!allowed) {
            errors.add("runtimeBinding is only valid on AGENT, WORKFLOW_REF, or TOOL nodes, found on: "
                    + node.getId());
            return;
        }
        RuntimeBindingValidator.validate("node " + node.getId(), node.getRuntimeBinding(), errors);
    }

    /** Validates runtime binding on a tool registry entry. */
    static void validateTool(ToolDefinition tool, List<String> errors) {
        RuntimeBindingValidator.validate("tool " + tool.getId(), tool.getRuntimeBinding(), errors);
    }

    /** Validates runtime contract and binding on an agent registry entry. */
    static void validateAgent(AgentDefinition agent, List<String> errors) {
        RuntimeBindingValidator.validate("agent " + agent.getId(), agent.getRuntimeBinding(), errors);
        if (agent.getRuntime() == null || agent.getRuntime().getExecutionModel() != ExecutionModel.CHILD_WORKFLOW) {
            errors.add("agent " + agent.getId() + ": runtime.executionModel CHILD_WORKFLOW is required");
        }
        validateWorkflowRuntimeContract("agent " + agent.getId(), agent.getRuntime(), errors);
    }

    /**
     * Checks required runtime contract fields (version, execution model) and delegates
     * to {@link WorkflowRuntimeDefinition#validate}.
     */
    static void validateWorkflowRuntimeContract(
            String context, WorkflowRuntimeDefinition runtime, List<String> errors) {
        if (runtime == null) {
            errors.add(context + ": runtime is required");
            return;
        }
        if (ValidationUtils.isBlank(runtime.getContractVersion())) {
            errors.add(context + ": runtime.contractVersion is required");
        }
        if (runtime.getExecutionModel() == null) {
            errors.add(context + ": runtime.executionModel is required");
        }
        WorkflowRuntimeDefinition.validate(runtime, context, errors);
    }
}
