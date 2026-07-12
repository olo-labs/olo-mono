/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Inline property panel on a Studio canvas node ({@code designer.nodeTypes.*.inlineProperties[]}).
 */
@JsonDeserialize(builder = InlinePropertyDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InlinePropertyDefinition {

    private final String id;
    private final String widget;
    private final String label;
    private final String binding;

    private InlinePropertyDefinition(Builder builder) {
        this.id = builder.id;
        this.widget = builder.widget;
        this.label = builder.label;
        this.binding = builder.binding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getWidget() {
        return widget;
    }

    public String getLabel() {
        return label;
    }

    public String getBinding() {
        return binding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InlinePropertyDefinition that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(widget, that.widget)
                && Objects.equals(label, that.label)
                && Objects.equals(binding, that.binding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, widget, label, binding);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String id;
        private String widget;
        private String label;
        private String binding;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder widget(String widget) {
            this.widget = widget;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder binding(String binding) {
            this.binding = binding;
            return this;
        }

        public InlinePropertyDefinition build() {
            return new InlinePropertyDefinition(this);
        }
    }
}
