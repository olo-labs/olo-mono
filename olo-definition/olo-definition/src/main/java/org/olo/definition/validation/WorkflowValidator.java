/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.validation.impl.WorkflowValidationCoordinator;
import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Public entry point for structural workflow validation (no runtime execution checks).
 *
 * <p>Validation rules are split across focused classes in
 * {@link org.olo.definition.validation.impl} so each stays under 200 lines. This facade
 * preserves the historical static API used by bootstrap, tests, and configuration generators.
 */
public final class WorkflowValidator {

    private WorkflowValidator() {
    }

    /**
     * Validates a workflow graph and returns all collected errors.
     */
    public static ValidationResult validate(WorkflowDefinition workflow) {
        return WorkflowValidationCoordinator.validate(workflow);
    }

    /**
     * Validates a workflow graph and throws {@link WorkflowValidationException} on failure.
     */
    public static void validateOrThrow(WorkflowDefinition workflow) {
        ValidationResult result = validate(workflow);
        if (!result.valid()) {
            throw new WorkflowValidationException(result.errors());
        }
    }
}
