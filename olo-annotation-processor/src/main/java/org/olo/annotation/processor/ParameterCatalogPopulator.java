/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloWorkflowParameter;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.ParameterUiDescriptor;
import org.olo.spi.catalog.ParameterSchemaMapping;

/** Materializes unified {@link ParameterDescriptor} entries from extension annotations. */
public final class ParameterCatalogPopulator {

    private ParameterCatalogPopulator() {}

    public static ParameterDescriptor fromProperty(OloProperty property) {
        ParameterSchemaMapping.MappedParameter schema =
                ParameterSchemaMapping.fromPropertyType(property.type().name(), property.secret());

        ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.id = property.name();
        descriptor.label = CatalogDefaults.materializePropertyLabel(property);
        descriptor.type = schema.jsonType();
        descriptor.description = CatalogDefaults.blankToNull(property.description());
        descriptor.required = property.required();
        descriptor.validation = ParameterValidationPopulator.from(property);
        descriptor.defaultValue = CatalogDefaults.parsePropertyDefault(descriptor.type, property.defaultValue());
        descriptor.values =
                "enum".equals(descriptor.type)
                        ? CatalogDefaults.optionalStringArray(property.enumValues())
                        : null;
        descriptor.examples = CatalogDefaults.optionalStringArray(property.examples());
        descriptor.visibleWhen = VisibleWhenPopulator.parse(property.visibleWhen());

        ParameterUiDescriptor ui = new ParameterUiDescriptor();
        ui.widget = schema.widget().catalogValue();
        ui.group = CatalogDefaults.optionalPropertyGroup(property.group());
        ui.help = CatalogDefaults.blankToNull(property.help());
        ui.placeholder = CatalogDefaults.blankToNull(property.placeholder());
        ui.order = CatalogDefaults.materializePropertyOrder(property.order());
        descriptor.ui = ui;
        return descriptor;
    }

    static ParameterDescriptor fromWorkflowParameter(OloWorkflowParameter parameter) {
        ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.id = parameter.name();
        descriptor.label = CatalogDefaults.materializeWorkflowParameterLabel(parameter);
        descriptor.type = parameter.type();
        descriptor.description = CatalogDefaults.blankToNull(parameter.description());
        descriptor.defaultValue = CatalogDefaults.parseWorkflowParameterDefault(parameter);
        descriptor.required = parameter.required();
        descriptor.validation = ParameterValidationPopulator.from(parameter);
        descriptor.visibleWhen = VisibleWhenPopulator.parse(parameter.visibleWhen());

        ParameterUiDescriptor ui = new ParameterUiDescriptor();
        ui.widget = parameter.widget().catalogValue();
        ui.group = CatalogDefaults.blankToNull(parameter.group());
        ui.help = CatalogDefaults.blankToNull(parameter.help());
        ui.placeholder = CatalogDefaults.blankToNull(parameter.placeholder());
        ui.order = CatalogDefaults.materializePropertyOrder(parameter.order());
        descriptor.ui = ui;
        return descriptor;
    }
}
