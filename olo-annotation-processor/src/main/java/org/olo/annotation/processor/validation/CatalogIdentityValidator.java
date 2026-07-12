/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import javax.lang.model.element.TypeElement;
import java.util.Map;

/**
 * Validates extension version strings and enforces unique global identifiers across the compilation unit.
 */
final class CatalogIdentityValidator {

    private final CatalogValidationContext context;

    CatalogIdentityValidator(CatalogValidationContext context) {
        this.context = context;
    }

    boolean requireVersion(TypeElement typeElement, String version) {
        if (version == null || version.isBlank()) {
            context.fail(
                    typeElement,
                    "OLO-AP-011",
                    "Extension version must not be blank on " + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }

    boolean requireUniqueNodeId(TypeElement typeElement, String globalId) {
        return requireUniqueId(typeElement, "OLO-AP-004", "node type", globalId, context.nodeIds());
    }

    boolean requireUniqueToolId(TypeElement typeElement, String globalId) {
        return requireUniqueId(typeElement, "OLO-AP-005", "tool id", globalId, context.toolIds());
    }

    boolean requireUniqueHookId(TypeElement typeElement, String globalId) {
        return requireUniqueId(typeElement, "OLO-AP-006", "hook implementation id", globalId, context.hookIds());
    }

    private boolean requireUniqueId(
            TypeElement typeElement, String code, String kindLabel, String id, Map<String, String> seen) {
        if (id == null || id.isBlank()) {
            context.fail(
                    typeElement,
                    code,
                    "Extension " + kindLabel + " must not be blank on " + typeElement.getQualifiedName());
            return false;
        }
        String previous = seen.putIfAbsent(id, typeElement.getQualifiedName().toString());
        if (previous != null) {
            context.fail(
                    typeElement,
                    code,
                    "Duplicate "
                            + kindLabel
                            + " \""
                            + id
                            + "\" on "
                            + typeElement.getQualifiedName()
                            + " (already declared on "
                            + previous
                            + ")");
            return false;
        }
        return true;
    }
}
