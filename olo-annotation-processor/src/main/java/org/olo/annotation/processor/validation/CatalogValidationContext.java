/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.validation;

import org.olo.annotation.processor.CatalogDefaults;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared compile-time validation state: error tracking, catalog identity options, and
 * duplicate-id registries for nodes, tools, and hooks.
 */
final class CatalogValidationContext {

    private final Messager messager;
    private final String catalogProvider;
    private final String catalogModule;
    private final Map<String, String> nodeIds = new LinkedHashMap<>();
    private final Map<String, String> toolIds = new LinkedHashMap<>();
    private final Map<String, String> hookIds = new LinkedHashMap<>();
    private boolean errors;

    CatalogValidationContext(Messager messager, String catalogModule, String catalogProvider) {
        this.messager = messager;
        this.catalogModule = catalogModule;
        this.catalogProvider = catalogProvider;
    }

    boolean hasErrors() {
        return errors;
    }

    Map<String, String> nodeIds() {
        return nodeIds;
    }

    Map<String, String> toolIds() {
        return toolIds;
    }

    Map<String, String> hookIds() {
        return hookIds;
    }

    String globalId(String localId, String annotationProvider) {
        String provider = CatalogDefaults.materializeProvider(annotationProvider, catalogProvider, catalogModule);
        return CatalogDefaults.materializeGlobalId(localId, provider);
    }

    void fail(Element element, String code, String message) {
        errors = true;
        messager.printMessage(Diagnostic.Kind.ERROR, "[" + code + "] " + message, element);
    }
}
