/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import org.olo.annotation.OloHook;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloTool;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Map;

/**
 * Facade that orchestrates focused compile-time validators for extension catalog annotations (OLO-AP-*).
 */
public final class ExtensionCatalogValidator {

    private final CatalogValidationContext context;
    private final CatalogSpiValidator spiValidator;
    private final CatalogIdentityValidator identityValidator;
    private final CatalogPortValidator portValidator;
    private final CatalogPropertyValidator propertyValidator;
    private final CatalogSchemaValidator schemaValidator;

    public ExtensionCatalogValidator(ProcessingEnvironment processingEnv) {
        Map<String, String> options = processingEnv.getOptions();
        String catalogModule = options.getOrDefault("olo.catalog.module", "extensions");
        String catalogProvider = options.getOrDefault("olo.catalog.provider", catalogModule);
        context = new CatalogValidationContext(processingEnv.getMessager(), catalogModule, catalogProvider);
        spiValidator = new CatalogSpiValidator(context);
        identityValidator = new CatalogIdentityValidator(context);
        portValidator = new CatalogPortValidator(context);
        propertyValidator = new CatalogPropertyValidator(context);
        schemaValidator = new CatalogSchemaValidator(context);
    }

    public boolean hasErrors() {
        return context.hasErrors();
    }

    public boolean validateNode(TypeElement typeElement, OloNode annotation) {
        boolean valid = true;
        valid &= identityValidator.requireVersion(typeElement, annotation.version());
        String globalId = context.globalId(annotation.type(), annotation.provider());
        valid &= spiValidator.validateNodeSpi(typeElement, globalId);
        valid &= identityValidator.requireUniqueNodeId(typeElement, globalId);
        valid &= portValidator.validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= portValidator.validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= propertyValidator.validateProperties(typeElement, annotation.configuration());
        valid &= schemaValidator.validateContractSchemas(
                typeElement, annotation.capabilityInputSchema(), annotation.capabilityOutputSchema());
        valid &= schemaValidator.validateDefaultTimeout(typeElement, annotation.defaultTimeout());
        return valid;
    }

    public boolean validateTool(TypeElement typeElement, OloTool annotation) {
        boolean valid = true;
        valid &= identityValidator.requireVersion(typeElement, annotation.version());
        String globalId = context.globalId(annotation.id(), annotation.provider());
        valid &= spiValidator.validateToolSpi(typeElement, globalId);
        valid &= identityValidator.requireUniqueToolId(typeElement, globalId);
        valid &= portValidator.validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= portValidator.validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= portValidator.requireCanvasPorts(
                typeElement, annotation.canvasPorts(), annotation.inputs(), annotation.outputs());
        valid &= propertyValidator.validateProperties(
                typeElement, annotation.arguments(), annotation.configuration());
        valid &= schemaValidator.validateContractSchemas(
                typeElement, annotation.capabilityInputSchema(), annotation.capabilityOutputSchema());
        valid &= schemaValidator.validateDefaultTimeout(typeElement, annotation.defaultTimeout());
        return valid;
    }

    public boolean validateHook(TypeElement typeElement, OloHook annotation) {
        boolean valid = true;
        valid &= identityValidator.requireVersion(typeElement, annotation.version());
        String globalId = context.globalId(annotation.implementationId(), annotation.provider());
        valid &= spiValidator.validateHookSpi(typeElement, globalId);
        valid &= identityValidator.requireUniqueHookId(typeElement, globalId);
        valid &= portValidator.validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= portValidator.validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= portValidator.requireCanvasPorts(
                typeElement, annotation.canvasPorts(), annotation.inputs(), annotation.outputs());
        return valid;
    }
}
