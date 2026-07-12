/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.parameter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Jackson builder and fluent factory for {@link WorkflowParameterDefinition}.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class WorkflowParameterDefinitionBuilder {

    String type;
    String label;
    String description;
    Object defaultValue;
    Boolean required;
    ParameterValidationDefinition validation;
    Map<String, String> visibleWhen;
    ParameterUiDefinition ui;

    public WorkflowParameterDefinitionBuilder type(String type) {
        this.type = type;
        return this;
    }

    @JsonAlias("schema")
    public WorkflowParameterDefinitionBuilder schema(String schema) {
        return type(schema);
    }

    public WorkflowParameterDefinitionBuilder label(String label) {
        this.label = label;
        return this;
    }

    public WorkflowParameterDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public WorkflowParameterDefinitionBuilder defaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public WorkflowParameterDefinitionBuilder required(Boolean required) {
        this.required = required;
        return this;
    }

    public WorkflowParameterDefinitionBuilder validation(ParameterValidationDefinition validation) {
        this.validation = validation;
        return this;
    }

    public WorkflowParameterDefinitionBuilder minimum(Double minimum) {
        validation(mergeValidation().minimum(minimum).build());
        return this;
    }

    public WorkflowParameterDefinitionBuilder maximum(Double maximum) {
        validation(mergeValidation().maximum(maximum).build());
        return this;
    }

    public WorkflowParameterDefinitionBuilder step(Double step) {
        validation(mergeValidation().step(step).build());
        return this;
    }

    public WorkflowParameterDefinitionBuilder minLength(Integer minLength) {
        validation(mergeValidation().minLength(minLength).build());
        return this;
    }

    public WorkflowParameterDefinitionBuilder maxLength(Integer maxLength) {
        validation(mergeValidation().maxLength(maxLength).build());
        return this;
    }

    public WorkflowParameterDefinitionBuilder visibleWhen(Map<String, String> visibleWhen) {
        this.visibleWhen = visibleWhen;
        return this;
    }

    public WorkflowParameterDefinitionBuilder ui(ParameterUiDefinition ui) {
        this.ui = ui;
        return this;
    }

    public WorkflowParameterDefinition build() {
        Objects.requireNonNull(type, "parameter type is required");
        return new WorkflowParameterDefinition(this);
    }

    private ParameterValidationDefinition.Builder mergeValidation() {
        ParameterValidationDefinition.Builder builder = ParameterValidationDefinition.builder();
        if (validation != null) {
            builder.minLength(validation.getMinLength())
                    .maxLength(validation.getMaxLength())
                    .minimum(validation.getMinimum())
                    .maximum(validation.getMaximum())
                    .step(validation.getStep());
        }
        return builder;
    }
}
