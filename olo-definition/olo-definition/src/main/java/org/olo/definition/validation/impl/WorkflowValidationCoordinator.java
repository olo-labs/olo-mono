/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.validation.ValidationResult;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates all workflow validation impl classes in dependency order.
 *
 * <p>Public entry point for {@link org.olo.definition.validation.WorkflowValidator} so impl
 * validators can remain package-private.
 */
public final class WorkflowValidationCoordinator {

    private WorkflowValidationCoordinator() {
    }

    /**
     * Runs the full validation pipeline and returns all collected errors.
     */
    public static ValidationResult validate(WorkflowDefinition workflow) {
        List<String> errors = new ArrayList<>();
        if (workflow == null) {
            return ValidationResult.failure(List.of("workflow must not be null"));
        }

        WorkflowCapabilityValidator.validateWorkflow(workflow, errors);
        WorkflowOrchestrationValidator.validateChildWorkflows(workflow, errors);
        WorkflowOrchestrationValidator.validateAvailableAgents(workflow, errors);
        WorkflowOrchestrationValidator.validateOrchestration(workflow, errors);
        WorkflowRuntimeValidator.validateWorkflow(workflow, errors);

        WorkflowValidationState state = WorkflowValidationState.create();
        WorkflowStructureValidator.validate(workflow, state, errors);

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failure(errors);
    }
}
