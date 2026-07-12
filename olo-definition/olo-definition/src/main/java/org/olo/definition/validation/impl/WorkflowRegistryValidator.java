/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.hook.HookValidator;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates workflow-level registries: tools, agents, hooks, and extensions.
 *
 * <p>Registry entries must have unique ids and satisfy type, capability, and runtime rules
 * delegated to {@link WorkflowCapabilityValidator} and {@link WorkflowRuntimeValidator}.
 */
final class WorkflowRegistryValidator {

    private WorkflowRegistryValidator() {
    }

    static void validate(WorkflowDefinition workflow, List<String> errors) {
        Set<String> registryIds = new HashSet<>();
        for (ToolDefinition tool : workflow.getTools()) {
            if (tool == null) {
                errors.add("tool entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(tool.getId())) {
                errors.add("tool id is required");
            } else if (!registryIds.add(tool.getId())) {
                errors.add("duplicate registry id: " + tool.getId());
            } else {
                validateTool(tool, errors);
            }
        }
        for (AgentDefinition agent : workflow.getAgents()) {
            if (agent == null) {
                errors.add("agent entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(agent.getId())) {
                errors.add("agent id is required");
            } else if (!registryIds.add(agent.getId())) {
                errors.add("duplicate registry id: " + agent.getId());
            } else {
                validateAgent(agent, errors);
            }
        }

        Set<String> hookIds = new HashSet<>();
        for (HookDefinition hook : workflow.getHooks()) {
            if (hook == null) {
                errors.add("hook entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(hook.getId())) {
                HookValidator.validate("workflow " + workflow.getId(), hook, errors);
            } else if (!hookIds.add(hook.getId())) {
                errors.add("duplicate hook id: " + hook.getId());
            } else {
                HookValidator.validate("workflow " + workflow.getId(), hook, errors);
            }
        }

        Set<String> extensionIds = new HashSet<>();
        for (var extension : workflow.getExtensions()) {
            if (extension == null) {
                errors.add("extension entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(extension.getId())) {
                errors.add("extension id is required");
            } else if (!extensionIds.add(extension.getId())) {
                errors.add("duplicate extension id: " + extension.getId());
            }
        }
    }

    private static void validateTool(ToolDefinition tool, List<String> errors) {
        if (!ToolDefinition.TYPE.equals(tool.getType())) {
            errors.add("tool " + tool.getId() + ": type must be " + ToolDefinition.TYPE);
        }
        WorkflowCapabilityValidator.validateTool(tool, errors);
        WorkflowRuntimeValidator.validateTool(tool, errors);
    }

    private static void validateAgent(AgentDefinition agent, List<String> errors) {
        if (!AgentDefinition.TYPE.equals(agent.getType())) {
            errors.add("agent " + agent.getId() + ": type must be " + AgentDefinition.TYPE);
        }
        WorkflowCapabilityValidator.validateAgent(agent, errors);
        if (agent.getWorkflow() == null) {
            errors.add("agent " + agent.getId() + ": workflow reference is required");
        } else if (ValidationUtils.isBlank(agent.getWorkflow().getWorkflowId())) {
            errors.add("agent " + agent.getId() + ": workflow.workflowId is required");
        }
        WorkflowRuntimeValidator.validateAgent(agent, errors);
    }
}
