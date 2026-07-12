/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.catalog;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloTool;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;
import org.olo.annotation.processor.CatalogComponentPopulator;
import org.olo.annotation.processor.CatalogContractPopulator;
import org.olo.annotation.processor.CatalogPortPopulator;
import org.olo.annotation.processor.CatalogRuntimePopulator;
import org.olo.annotation.processor.DesignerPopulator;
import org.olo.annotation.processor.ParameterCatalogPopulator;
import org.olo.annotation.processor.RuntimeBindingBuilder;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds {@link ToolDescriptor} entries and runtime bindings from {@link OloTool}-annotated types.
 */
public final class CatalogToolCollector {

    private final String catalogProvider;
    private final String catalogModule;
    private final List<RuntimeBindingDescriptor> runtimeBindings;

    public CatalogToolCollector(
            String catalogProvider, String catalogModule, List<RuntimeBindingDescriptor> runtimeBindings) {
        this.catalogProvider = catalogProvider;
        this.catalogModule = catalogModule;
        this.runtimeBindings = runtimeBindings;
    }

    public ToolDescriptor collect(TypeElement typeElement, OloTool annotation) {
        ToolDescriptor descriptor = new ToolDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "TOOL",
                annotation.id(),
                typeElement,
                annotation.name(),
                annotation.description(),
                annotation.category(),
                annotation.emoji(),
                annotation.tags(),
                annotation.examples(),
                annotation.featured(),
                annotation.deprecated(),
                annotation.stability(),
                annotation.experimental(),
                annotation.version(),
                annotation.provider(),
                catalogProvider,
                catalogModule);
        runtimeBindings.add(
                RuntimeBindingBuilder.create(
                        "TOOL", descriptor.id, typeElement, "org.olo.spi.tool.Tool"));
        descriptor.designer =
                DesignerPopulator.from(annotation.designer(), annotation.category(), annotation.tags());
        descriptor.inputs = CatalogPortPopulator.resolveInputs(annotation.inputs(), annotation.canvasPorts());
        descriptor.outputs = CatalogPortPopulator.resolveOutputs(annotation.outputs(), annotation.canvasPorts());
        descriptor.parameters = mergeToolParameters(annotation.arguments(), annotation.configuration());
        descriptor.contract = CatalogContractPopulator.create(
                annotation.capabilityInputSchema(), annotation.capabilityOutputSchema());
        descriptor.runtime = CatalogRuntimePopulator.create(
                annotation.runtimeContractVersion(),
                annotation.executionModel(),
                annotation.retryable(),
                annotation.timeoutAware(),
                annotation.defaultTimeout(),
                annotation.defaultRetryPolicy(),
                annotation.supportsAsyncCompletion(),
                annotation.supportsHeartbeat(),
                annotation.supportsDebugging(),
                annotation.supportsReplay(),
                annotation.supportsCheckpointing());
        return descriptor;
    }

    private static List<ParameterDescriptor> mergeToolParameters(
            OloProperty[] arguments, OloProperty[] configuration) {
        List<ParameterDescriptor> out = new ArrayList<>();
        if (arguments != null) {
            for (OloProperty property : arguments) {
                out.add(ParameterCatalogPopulator.fromProperty(property));
            }
        }
        if (configuration != null) {
            for (OloProperty property : configuration) {
                out.add(ParameterCatalogPopulator.fromProperty(property));
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        out.sort(Comparator.comparing(
                d -> d.ui == null ? null : d.ui.order, Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }
}
