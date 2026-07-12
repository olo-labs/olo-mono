/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;

/**
 * Workflow tuning parameter ({@code parameters} on {@link org.olo.definition.workflow.WorkflowDefinition})
 * with optional Studio UI metadata. Runtime consumers use {@link #getType()}, {@link #getDefaultValue()},
 * and bounds; editors also read {@link #getUi()} and presentation fields.
 */
@JsonDeserialize(using = WorkflowParameterDefinitionDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "type",
    "label",
    "description",
    "defaultValue",
    "required",
    "validation",
    "visibleWhen",
    "ui"
})
public final class WorkflowParameterDefinition {

    private final String type;
    private final String label;
    private final String description;
    private final Object defaultValue;
    private final Boolean required;
    private final ParameterValidationDefinition validation;
    private final Map<String, String> visibleWhen;
    private final ParameterUiDefinition ui;

    WorkflowParameterDefinition(WorkflowParameterDefinitionBuilder builder) {
        this.type = builder.type;
        this.label = builder.label;
        this.description = builder.description;
        this.defaultValue = builder.defaultValue;
        this.required = builder.required;
        this.validation = builder.validation;
        this.visibleWhen = builder.visibleWhen;
        this.ui = builder.ui;
    }

    public static WorkflowParameterDefinitionBuilder builder() {
        return new WorkflowParameterDefinitionBuilder();
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /** @deprecated use {@link #getType()} — legacy presets used {@code schema}. */
    @Deprecated
    @JsonIgnore
    public String getSchema() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public ParameterValidationDefinition getValidation() {
        return validation;
    }

    /** @deprecated use {@link #getValidation()} {@code .getMinimum()}. */
    @Deprecated
    @JsonIgnore
    public Double getMinimum() {
        return validation == null ? null : validation.getMinimum();
    }

    /** @deprecated use {@link #getValidation()} {@code .getMaximum()}. */
    @Deprecated
    @JsonIgnore
    public Double getMaximum() {
        return validation == null ? null : validation.getMaximum();
    }

    /** @deprecated use {@link #getValidation()} {@code .getStep()}. */
    @Deprecated
    @JsonIgnore
    public Double getStep() {
        return validation == null ? null : validation.getStep();
    }

    public Map<String, String> getVisibleWhen() {
        return visibleWhen;
    }

    public ParameterUiDefinition getUi() {
        return ui;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowParameterDefinition that)) {
            return false;
        }
        return Objects.equals(type, that.type)
                && Objects.equals(label, that.label)
                && Objects.equals(description, that.description)
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(required, that.required)
                && Objects.equals(validation, that.validation)
                && Objects.equals(visibleWhen, that.visibleWhen)
                && Objects.equals(ui, that.ui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, label, description, defaultValue, required, validation, visibleWhen, ui);
    }
}
