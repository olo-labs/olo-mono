/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.catalog;

import org.olo.annotation.OloHook;
import org.olo.annotation.OloHookPhase;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.processor.CatalogComponentPopulator;
import org.olo.annotation.processor.CatalogPortPopulator;
import org.olo.annotation.processor.DesignerPopulator;
import org.olo.annotation.processor.RuntimeBindingBuilder;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link HookDescriptor} entries and runtime bindings from {@link OloHook}-annotated types.
 */
public final class CatalogHookCollector {

    private final String catalogProvider;
    private final String catalogModule;
    private final List<RuntimeBindingDescriptor> runtimeBindings;

    public CatalogHookCollector(
            String catalogProvider, String catalogModule, List<RuntimeBindingDescriptor> runtimeBindings) {
        this.catalogProvider = catalogProvider;
        this.catalogModule = catalogModule;
        this.runtimeBindings = runtimeBindings;
    }

    public HookDescriptor collect(TypeElement typeElement, OloHook annotation) {
        HookDescriptor descriptor = new HookDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "HOOK",
                annotation.implementationId(),
                typeElement,
                annotation.name(),
                annotation.description(),
                annotation.category(),
                annotation.emoji(),
                annotation.tags(),
                new String[0],
                false,
                annotation.deprecated(),
                annotation.stability(),
                annotation.experimental(),
                annotation.version(),
                annotation.provider(),
                catalogProvider,
                catalogModule);
        runtimeBindings.add(
                RuntimeBindingBuilder.create(
                        "HOOK", descriptor.id, typeElement, "org.olo.spi.hook.Hook"));
        descriptor.designer =
                DesignerPopulator.from(annotation.designer(), annotation.category(), annotation.tags());
        descriptor.inputs = CatalogPortPopulator.resolveInputs(annotation.inputs(), annotation.canvasPorts());
        descriptor.outputs = CatalogPortPopulator.resolveOutputs(annotation.outputs(), annotation.canvasPorts());
        descriptor.phases = phases(annotation.phases());
        return descriptor;
    }

    private static List<String> phases(OloHookPhase[] phases) {
        if (phases == null || phases.length == 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (OloHookPhase phase : phases) {
            out.add(phase.name());
        }
        return out;
    }
}
