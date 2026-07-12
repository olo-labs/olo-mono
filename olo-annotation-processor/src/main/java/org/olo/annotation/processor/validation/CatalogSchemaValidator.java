/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import org.olo.annotation.processor.CatalogDefaults;

import javax.lang.model.element.TypeElement;

/**
 * Validates JSON capability schemas and ISO-8601 default timeout durations on extension annotations.
 */
final class CatalogSchemaValidator {

    private final CatalogValidationContext context;

    CatalogSchemaValidator(CatalogValidationContext context) {
        this.context = context;
    }

    boolean validateContractSchemas(TypeElement typeElement, String inputSchema, String outputSchema) {
        boolean valid = true;
        valid &= validateContractSchema(typeElement, "capabilityInputSchema", inputSchema);
        valid &= validateContractSchema(typeElement, "capabilityOutputSchema", outputSchema);
        return valid;
    }

    boolean validateDefaultTimeout(TypeElement typeElement, String duration) {
        if (duration == null || duration.isBlank()) {
            return true;
        }
        try {
            CatalogDefaults.materializeIsoDuration(duration);
            return true;
        } catch (IllegalArgumentException e) {
            context.fail(
                    typeElement,
                    "OLO-AP-013",
                    "defaultTimeout must be a valid ISO-8601 duration on " + typeElement.getQualifiedName());
            return false;
        }
    }

    private boolean validateContractSchema(TypeElement typeElement, String fieldLabel, String json) {
        if (json == null || json.isBlank()) {
            return true;
        }
        try {
            CatalogDefaults.parseJsonSchema(json);
            return true;
        } catch (IllegalArgumentException e) {
            context.fail(
                    typeElement,
                    "OLO-AP-012",
                    fieldLabel + " must be a valid JSON object on " + typeElement.getQualifiedName());
            return false;
        }
    }
}
