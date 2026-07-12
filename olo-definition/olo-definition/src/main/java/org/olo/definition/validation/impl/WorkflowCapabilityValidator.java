/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityValidator;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/**
 * Validates capability declarations on workflows, nodes, tools, and agents.
 *
 * <p>Capabilities describe what a workflow or registry entry is allowed to do; this validator
 * delegates to {@link CapabilityValidator} with the appropriate context for each scope.
 */
final class WorkflowCapabilityValidator {

    private WorkflowCapabilityValidator() {
    }

    /** Validates the top-level workflow capability block. */
    static void validateWorkflow(WorkflowDefinition workflow, List<String> errors) {
        CapabilityValidator.validate(
                workflow.getId(),
                workflow.getCapability(),
                CapabilityValidator.Context.WORKFLOW,
                errors);
    }

    /** Validates an optional per-node capability override. */
    static void validateNode(NodeDefinition node, List<String> errors) {
        if (node.getCapability() != null) {
            CapabilityValidator.validate(
                    node.getId(), node.getCapability(), CapabilityValidator.Context.NODE, errors);
        }
    }

    /** Validates capability on a tool registry entry. */
    static void validateTool(ToolDefinition tool, List<String> errors) {
        CapabilityValidator.validate(
                tool.getId(), tool.getCapability(), CapabilityValidator.Context.TOOL, errors);
    }

    /** Validates capability on an agent registry entry. */
    static void validateAgent(AgentDefinition agent, List<String> errors) {
        CapabilityValidator.validate(
                agent.getId(), agent.getCapability(), CapabilityValidator.Context.AGENT, errors);
    }
}
