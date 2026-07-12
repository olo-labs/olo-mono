/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import java.util.Objects;

/**
 * Value equality and string representation for {@link WorkflowDefinition}.
 */
final class WorkflowDefinitionEquality {

    private WorkflowDefinitionEquality() {
    }

    static boolean equals(WorkflowDefinition left, Object other) {
        if (left == other) {
            return true;
        }
        if (!(other instanceof WorkflowDefinition that)) {
            return false;
        }
        return Objects.equals(left.getId(), that.getId())
                && Objects.equals(left.isEnabled(), that.isEnabled())
                && Objects.equals(left.isDefault(), that.isDefault())
                && Objects.equals(left.getLabel(), that.getLabel())
                && Objects.equals(left.getRole(), that.getRole())
                && Objects.equals(left.getShortDescription(), that.getShortDescription())
                && Objects.equals(left.getEmoji(), that.getEmoji())
                && Objects.equals(left.getDesigner(), that.getDesigner())
                && Objects.equals(left.getQueue(), that.getQueue())
                && Objects.equals(left.getWorkflowType(), that.getWorkflowType())
                && Objects.equals(left.isRunAgain(), that.isRunAgain())
                && Objects.equals(left.getLongDescription(), that.getLongDescription())
                && Objects.equals(left.isExternalWorkflow(), that.isExternalWorkflow())
                && Objects.equals(left.isChildWorkflow(), that.isChildWorkflow())
                && Objects.equals(left.getChildWorkflows(), that.getChildWorkflows())
                && Objects.equals(left.getAvailableAgents(), that.getAvailableAgents())
                && Objects.equals(left.getVersion(), that.getVersion())
                && Objects.equals(left.getNodes(), that.getNodes())
                && Objects.equals(left.getEdges(), that.getEdges())
                && Objects.equals(left.getInputs(), that.getInputs())
                && Objects.equals(left.getState(), that.getState())
                && Objects.equals(left.getParameters(), that.getParameters())
                && Objects.equals(left.getVariables(), that.getVariables())
                && Objects.equals(left.getModelProviders(), that.getModelProviders())
                && Objects.equals(left.getModelRouting(), that.getModelRouting())
                && Objects.equals(left.getExtensions(), that.getExtensions())
                && Objects.equals(left.getMetadata(), that.getMetadata())
                && Objects.equals(left.getCapability(), that.getCapability())
                && Objects.equals(left.getRuntime(), that.getRuntime())
                && Objects.equals(left.getRuntimeBinding(), that.getRuntimeBinding())
                && Objects.equals(left.getTools(), that.getTools())
                && Objects.equals(left.getAgents(), that.getAgents())
                && Objects.equals(left.getHooks(), that.getHooks());
    }

    static int hashCode(WorkflowDefinition workflow) {
        return Objects.hash(
                workflow.getId(),
                workflow.isEnabled(),
                workflow.getLabel(),
                workflow.getRole(),
                workflow.getShortDescription(),
                workflow.getEmoji(),
                workflow.getDesigner(),
                workflow.getQueue(),
                workflow.getWorkflowType(),
                workflow.isRunAgain(),
                workflow.getLongDescription(),
                workflow.isExternalWorkflow(),
                workflow.isChildWorkflow(),
                workflow.getChildWorkflows(),
                workflow.getAvailableAgents(),
                workflow.getVersion(),
                workflow.getNodes(),
                workflow.getEdges(),
                workflow.getInputs(),
                workflow.getState(),
                workflow.getParameters(),
                workflow.getVariables(),
                workflow.getModelProviders(),
                workflow.getModelRouting(),
                workflow.getExtensions(),
                workflow.getMetadata(),
                workflow.getCapability(),
                workflow.getRuntime(),
                workflow.getRuntimeBinding(),
                workflow.getTools(),
                workflow.getAgents(),
                workflow.getHooks());
    }

    static String toString(WorkflowDefinition workflow) {
        return "WorkflowDefinition{id='" + workflow.getId() + "', label='" + workflow.getLabel()
                + "', role='" + workflow.getRole() + "', nodes=" + workflow.getNodes().size() + "}";
    }
}
