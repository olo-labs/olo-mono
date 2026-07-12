/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.olo.annotation.OloCatalogLocations;
import org.olo.annotation.OloHook;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloTool;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.NodeDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;
import org.olo.annotation.processor.catalog.CatalogDocumentWriter;
import org.olo.annotation.processor.catalog.CatalogHookCollector;
import org.olo.annotation.processor.catalog.CatalogNodeCollector;
import org.olo.annotation.processor.catalog.CatalogToolCollector;
import org.olo.annotation.processor.validation.ExtensionCatalogValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates {@link OloCatalogLocations} JSON catalogs from {@link OloNode}, {@link OloTool}, and {@link OloHook}.
 */
@SupportedAnnotationTypes({
        "org.olo.annotation.OloNode",
        "org.olo.annotation.OloTool",
        "org.olo.annotation.OloHook"
})
@SupportedOptions({"olo.catalog.module", "olo.catalog.provider"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class OloExtensionCatalogProcessor extends AbstractProcessor {

    private final List<NodeDescriptor> nodes = new ArrayList<>();
    private final List<ToolDescriptor> tools = new ArrayList<>();
    private final List<HookDescriptor> hooks = new ArrayList<>();
    private final List<RuntimeBindingDescriptor> runtimeBindings = new ArrayList<>();

    private ExtensionCatalogValidator validator;
    private CatalogNodeCollector nodeCollector;
    private CatalogToolCollector toolCollector;
    private CatalogHookCollector hookCollector;
    private CatalogDocumentWriter documentWriter;
    private String catalogModule;
    private boolean written;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        validator = new ExtensionCatalogValidator(processingEnv);
        Map<String, String> options = processingEnv.getOptions();
        catalogModule = options.getOrDefault("olo.catalog.module", "extensions");
        String catalogProvider = options.getOrDefault("olo.catalog.provider", catalogModule);
        nodeCollector = new CatalogNodeCollector(catalogProvider, catalogModule, runtimeBindings);
        toolCollector = new CatalogToolCollector(catalogProvider, catalogModule, runtimeBindings);
        hookCollector = new CatalogHookCollector(catalogProvider, catalogModule, runtimeBindings);
        documentWriter = new CatalogDocumentWriter(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (validator.hasErrors()) {
                return false;
            }
            if (!written
                    && (!nodes.isEmpty() || !tools.isEmpty() || !hooks.isEmpty() || !runtimeBindings.isEmpty())) {
                documentWriter.write(catalogModule, nodes, tools, hooks, runtimeBindings);
                written = true;
            }
            return false;
        }

        collectNodes(roundEnv);
        collectTools(roundEnv);
        collectHooks(roundEnv);
        return false;
    }

    private void collectNodes(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(OloNode.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloNode annotation = element.getAnnotation(OloNode.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateNode(typeElement, annotation)) {
                nodes.add(nodeCollector.collect(typeElement, annotation));
            }
        }
    }

    private void collectTools(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(OloTool.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloTool annotation = element.getAnnotation(OloTool.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateTool(typeElement, annotation)) {
                tools.add(toolCollector.collect(typeElement, annotation));
            }
        }
    }

    private void collectHooks(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(OloHook.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloHook annotation = element.getAnnotation(OloHook.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateHook(typeElement, annotation)) {
                hooks.add(hookCollector.collect(typeElement, annotation));
            }
        }
    }
}
