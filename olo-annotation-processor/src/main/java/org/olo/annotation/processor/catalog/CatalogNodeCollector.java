/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.catalog;

import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.NodeDescriptor;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.PortDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.processor.CatalogComponentPopulator;
import org.olo.annotation.processor.CatalogContractPopulator;
import org.olo.annotation.processor.CatalogPortPopulator;
import org.olo.annotation.processor.CatalogRuntimePopulator;
import org.olo.annotation.processor.ConnectionPolicyPopulator;
import org.olo.annotation.processor.DesignerPopulator;
import org.olo.annotation.processor.ParameterCatalogPopulator;
import org.olo.annotation.processor.RuntimeBindingBuilder;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds {@link NodeDescriptor} entries and runtime bindings from {@link OloNode}-annotated types.
 */
public final class CatalogNodeCollector {

    private final String catalogProvider;
    private final String catalogModule;
    private final List<RuntimeBindingDescriptor> runtimeBindings;

    public CatalogNodeCollector(
            String catalogProvider, String catalogModule, List<RuntimeBindingDescriptor> runtimeBindings) {
        this.catalogProvider = catalogProvider;
        this.catalogModule = catalogModule;
        this.runtimeBindings = runtimeBindings;
    }

    public NodeDescriptor collect(TypeElement typeElement, OloNode annotation) {
        NodeDescriptor descriptor = new NodeDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "NODE",
                annotation.type(),
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
                        "NODE", descriptor.id, typeElement, "org.olo.spi.node.Node"));
        descriptor.designer = DesignerPopulator.from(
                annotation.designer(),
                annotation.category(),
                annotation.tags(),
                annotation.nodeShape(),
                annotation.uiWidth(),
                annotation.uiHeight());
        descriptor.connectionPolicy = ConnectionPolicyPopulator.from(annotation.connectionPolicy());
        descriptor.inputs = materializeNodePorts(annotation.inputs(), false);
        descriptor.outputs = materializeNodePorts(annotation.outputs(), true);
        descriptor.parameters = parameters(annotation.configuration());
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

    private static List<PortDescriptor> materializeNodePorts(OloPort[] ports, boolean output) {
        if (ports == null || ports.length == 0) {
            return List.of();
        }
        List<PortDescriptor> out = new ArrayList<>();
        for (OloPort port : ports) {
            out.add(CatalogPortPopulator.materializePort(port, output));
        }
        return out;
    }

    private static List<ParameterDescriptor> parameters(OloProperty[] properties) {
        if (properties == null || properties.length == 0) {
            return List.of();
        }
        List<ParameterDescriptor> out = new ArrayList<>();
        for (OloProperty property : properties) {
            out.add(ParameterCatalogPopulator.fromProperty(property));
        }
        sortParameters(out);
        return out;
    }

    private static void sortParameters(List<ParameterDescriptor> parameters) {
        parameters.sort(Comparator.comparing(
                d -> d.ui == null ? null : d.ui.order, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
