/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.port;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Jackson builder and fluent factory for {@link PortDefinition}.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class PortDefinitionBuilder {

    String id;
    String label;
    String schema;
    String type;
    String acceptType;
    PortDirection direction;
    boolean required;
    int minConnections;
    Integer maxConnections;
    String shortDescription;
    PortUiDefinition ui;

    public PortDefinitionBuilder id(String id) {
        this.id = id;
        return this;
    }

    @JsonAlias("name")
    public PortDefinitionBuilder label(String label) {
        this.label = label;
        return this;
    }

    /** @deprecated use {@link #label(String)} */
    @Deprecated
    public PortDefinitionBuilder name(String name) {
        if (this.label == null || this.label.isBlank()) {
            this.label = name;
        }
        return this;
    }

    public PortDefinitionBuilder schema(String schema) {
        this.schema = schema;
        return this;
    }

    public PortDefinitionBuilder type(String type) {
        this.type = type;
        return this;
    }

    public PortDefinitionBuilder acceptType(String acceptType) {
        this.acceptType = acceptType;
        return this;
    }

    public PortDefinitionBuilder direction(PortDirection direction) {
        this.direction = direction;
        return this;
    }

    public PortDefinitionBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public PortDefinitionBuilder minConnections(int minConnections) {
        this.minConnections = minConnections;
        return this;
    }

    public PortDefinitionBuilder maxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public PortDefinitionBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    public PortDefinitionBuilder ui(PortUiDefinition ui) {
        this.ui = ui;
        return this;
    }

    public PortDefinition build() {
        Objects.requireNonNull(label, "port label is required");
        Objects.requireNonNull(schema, "port schema is required");
        Objects.requireNonNull(direction, "port direction is required");
        if (id == null) {
            id = label;
        }
        if (minConnections < 0) {
            throw new IllegalArgumentException("minConnections must be >= 0");
        }
        if (maxConnections != null && maxConnections < minConnections) {
            throw new IllegalArgumentException("maxConnections must be >= minConnections");
        }
        return new PortDefinition(this);
    }
}
