/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Map;

/**
 * Validates that catalog annotation identifiers match their corresponding SPI annotations
 * ({@code @NodeType}, {@code @ToolId}, {@code @ImplementationId}).
 */
final class CatalogSpiValidator {

    private static final String NODE_TYPE_ANNOTATION = "org.olo.spi.annotation.NodeType";
    private static final String TOOL_ID_ANNOTATION = "org.olo.spi.annotation.ToolId";
    private static final String IMPLEMENTATION_ID_ANNOTATION = "org.olo.spi.annotation.ImplementationId";

    private final CatalogValidationContext context;

    CatalogSpiValidator(CatalogValidationContext context) {
        this.context = context;
    }

    boolean validateNodeSpi(TypeElement typeElement, String globalId) {
        return requireSpiMatch(
                typeElement, "OLO-AP-001", "@OloNode.type", globalId, "@NodeType", NODE_TYPE_ANNOTATION);
    }

    boolean validateToolSpi(TypeElement typeElement, String globalId) {
        return requireSpiMatch(
                typeElement, "OLO-AP-002", "@OloTool.id", globalId, "@ToolId", TOOL_ID_ANNOTATION);
    }

    boolean validateHookSpi(TypeElement typeElement, String globalId) {
        return requireSpiMatch(
                typeElement,
                "OLO-AP-003",
                "@OloHook.implementationId",
                globalId,
                "@ImplementationId",
                IMPLEMENTATION_ID_ANNOTATION);
    }

    private boolean requireSpiMatch(
            TypeElement typeElement,
            String code,
            String catalogLabel,
            String catalogValue,
            String spiLabel,
            String spiAnnotationFqn) {
        String spiValue = readAnnotationValue(typeElement, spiAnnotationFqn, "value");
        if (spiValue != null) {
            spiValue = context.globalId(spiValue, null);
        }
        if (spiValue == null) {
            context.fail(
                    typeElement,
                    code,
                    catalogLabel
                            + " \""
                            + catalogValue
                            + "\" requires "
                            + spiLabel
                            + " with the same value on "
                            + typeElement.getQualifiedName());
            return false;
        }
        if (!catalogValue.equals(spiValue)) {
            context.fail(
                    typeElement,
                    code,
                    catalogLabel
                            + " \""
                            + catalogValue
                            + "\" does not match "
                            + spiLabel
                            + " \""
                            + spiValue
                            + "\" on "
                            + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }

    private String readAnnotationValue(TypeElement typeElement, String annotationFqn, String memberName) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            if (!isAnnotationType(mirror, annotationFqn)) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(memberName)) {
                    Object value = entry.getValue().getValue();
                    return value != null ? value.toString() : null;
                }
            }
        }
        return null;
    }

    private static boolean isAnnotationType(AnnotationMirror mirror, String qualifiedName) {
        Element element = mirror.getAnnotationType().asElement();
        return element instanceof TypeElement annotationType
                && annotationType.getQualifiedName().contentEquals(qualifiedName);
    }
}
