/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import org.olo.annotation.OloCanvasPorts;
import org.olo.annotation.OloPort;

import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates port declarations on extension annotations, including canvas port profile requirements.
 */
final class CatalogPortValidator {

    private final CatalogValidationContext context;

    CatalogPortValidator(CatalogValidationContext context) {
        this.context = context;
    }

    boolean validatePorts(TypeElement typeElement, String fieldLabel, OloPort[] ports) {
        if (ports == null || ports.length == 0) {
            return true;
        }
        boolean valid = true;
        Set<String> seen = new HashSet<>();
        for (OloPort port : ports) {
            String id = port.id();
            if (id == null || id.isBlank()) {
                context.fail(
                        typeElement,
                        "OLO-AP-008",
                        "Port id must not be blank in " + fieldLabel + " on " + typeElement.getQualifiedName());
                valid = false;
                continue;
            }
            if (!seen.add(id)) {
                context.fail(
                        typeElement,
                        "OLO-AP-008",
                        "Duplicate port id \""
                                + id
                                + "\" in "
                                + fieldLabel
                                + " on "
                                + typeElement.getQualifiedName());
                valid = false;
            }
        }
        return valid;
    }

    boolean requireCanvasPorts(
            TypeElement typeElement, OloCanvasPorts profile, OloPort[] inputs, OloPort[] outputs) {
        boolean hasExplicit = (inputs != null && inputs.length > 0) || (outputs != null && outputs.length > 0);
        if (hasExplicit) {
            return true;
        }
        if (profile == null || profile == OloCanvasPorts.NONE) {
            context.fail(
                    typeElement,
                    "OLO-AP-009",
                    "Declare canvasPorts or explicit inputs/outputs for graph components on "
                            + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }
}
