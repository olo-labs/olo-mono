/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.planner.WorkflowPlannerMetadata;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates planner orchestration metadata: child workflows, available agents, and delegation.
 *
 * <p>When a workflow declares {@code availableAgents}, it must also declare how agents are selected
 * and how runtime delegation is configured so the planner can route work correctly.
 */
final class WorkflowOrchestrationValidator {

    private WorkflowOrchestrationValidator() {
    }

    /** Validates declared child workflow references for id presence and uniqueness. */
    static void validateChildWorkflows(WorkflowDefinition workflow, List<String> errors) {
        Set<String> childIds = new HashSet<>();
        for (ChildWorkflowDefinition child : workflow.getChildWorkflows()) {
            if (child == null) {
                errors.add("child workflow entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(child.getWorkflowId())) {
                errors.add("child workflow workflowId is required");
            } else if (!childIds.add(child.getWorkflowId())) {
                errors.add("duplicate child workflow workflowId: " + child.getWorkflowId());
            }
        }
    }

    /** Validates available agent catalog entries and required planner metadata. */
    static void validateAvailableAgents(WorkflowDefinition workflow, List<String> errors) {
        Set<String> agentIds = new HashSet<>();
        for (AgentReferenceDefinition agent : workflow.getAvailableAgents()) {
            if (agent == null) {
                errors.add("availableAgents entry must not be null");
                continue;
            }
            String agentId = agent.getId();
            if (ValidationUtils.isBlank(agentId)) {
                errors.add("availableAgents id is required");
            } else if (!agentIds.add(agentId)) {
                errors.add("duplicate availableAgents id: " + agentId);
            }
        }
        if (!workflow.getAvailableAgents().isEmpty()
                && workflow.getMetadata().get(WorkflowPlannerMetadata.AGENT_SELECTION_STRATEGY) == null) {
            errors.add("workflow " + workflow.getId()
                    + ": metadata.agentSelectionStrategy is required when availableAgents is set");
        }
        WorkflowPlannerMetadata.validateAgentSelectionStrategy(
                workflow.getId(), workflow.getMetadata(), errors);
    }

    /**
     * Ensures runtime delegation is configured when the workflow exposes an agent catalog.
     */
    static void validateOrchestration(WorkflowDefinition workflow, List<String> errors) {
        String context = "workflow " + workflow.getId();
        if (!workflow.getAvailableAgents().isEmpty()
                && (workflow.getRuntime() == null || workflow.getRuntime().getDelegation() == null)) {
            errors.add(context + ": runtime.delegation is required when availableAgents is set");
        }
    }
}
