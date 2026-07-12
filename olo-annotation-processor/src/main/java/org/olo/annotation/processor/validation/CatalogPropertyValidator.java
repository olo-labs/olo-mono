/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;

import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates configuration and argument property declarations on extension annotations.
 */
final class CatalogPropertyValidator {

    private final CatalogValidationContext context;

    CatalogPropertyValidator(CatalogValidationContext context) {
        this.context = context;
    }

    boolean validateProperties(TypeElement typeElement, OloProperty[]... propertyLists) {
        boolean valid = true;
        Map<String, String> seen = new LinkedHashMap<>();
        for (OloProperty[] properties : propertyLists) {
            if (properties == null || properties.length == 0) {
                continue;
            }
            for (OloProperty property : properties) {
                String name = property.name();
                if (name == null || name.isBlank()) {
                    context.fail(
                            typeElement,
                            "OLO-AP-007",
                            "Property name must not be blank on " + typeElement.getQualifiedName());
                    valid = false;
                    continue;
                }
                String previous = seen.put(name, typeElement.getQualifiedName().toString());
                if (previous != null) {
                    context.fail(
                            typeElement,
                            "OLO-AP-007",
                            "Duplicate property name \""
                                    + name
                                    + "\" on "
                                    + typeElement.getQualifiedName());
                    valid = false;
                }
                if (property.type() == OloPropertyType.ENUM
                        && (property.enumValues() == null || property.enumValues().length == 0)) {
                    context.fail(
                            typeElement,
                            "OLO-AP-009",
                            "Property \""
                                    + name
                                    + "\" has type ENUM but no enumValues on "
                                    + typeElement.getQualifiedName());
                    valid = false;
                }
            }
        }
        return valid;
    }
}
