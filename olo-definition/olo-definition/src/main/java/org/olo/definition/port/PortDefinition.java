/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.port;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

/**
 * Declares a typed connection point on a node. Edges reference ports by {@link #getId()}.
 */
@JsonDeserialize(builder = PortDefinitionBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortDefinition {

    private final String id;
    private final String label;
    private final String schema;
    private final String type;
    private final String acceptType;
    private final PortDirection direction;
    private final boolean required;
    private final int minConnections;
    private final Integer maxConnections;
    private final String shortDescription;
    private final PortUiDefinition ui;

    PortDefinition(PortDefinitionBuilder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.schema = builder.schema;
        this.type = builder.type;
        this.acceptType = builder.acceptType;
        this.direction = builder.direction;
        this.required = builder.required;
        this.minConnections = builder.minConnections;
        this.maxConnections = builder.maxConnections;
        this.shortDescription = builder.shortDescription;
        this.ui = builder.ui == null ? PortUiDefinition.forDirection(builder.direction) : builder.ui;
    }

    public static PortDefinitionBuilder builder() {
        return new PortDefinitionBuilder();
    }

    public static PortDefinition inputPort(String id, String schema) {
        return builder().id(id).label(id).schema(schema).direction(PortDirection.INPUT).build();
    }

    public static PortDefinition outputPort(String id, String schema) {
        return builder().id(id).label(id).schema(schema).direction(PortDirection.OUTPUT).build();
    }

    public String getId() {
        return id;
    }

    /** Display label in Studio; legacy callers may still use {@link #getName()}. */
    public String getLabel() {
        return label;
    }

    /** @deprecated use {@link #getLabel()} */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return label;
    }

    public String getSchema() {
        return schema;
    }

    /** Wire type produced (outputs) or declared on the port. Falls back to {@link #getSchema()} when unset. */
    public String getType() {
        return type;
    }

    /** Types this input port accepts (e.g. {@code any}, {@code string}). Falls back to {@link #getType()} then schema. */
    public String getAcceptType() {
        return acceptType;
    }

    public PortDirection getDirection() {
        return direction;
    }

    public boolean isRequired() {
        return required;
    }

    public int getMinConnections() {
        return minConnections;
    }

    /** {@code null} means no upper bound. */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /** Brief Studio tooltip text describing what this port carries. */
    public String getShortDescription() {
        return shortDescription;
    }

    public PortUiDefinition getUi() {
        return ui;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortDefinition that)) {
            return false;
        }
        return required == that.required
                && minConnections == that.minConnections
                && Objects.equals(id, that.id)
                && Objects.equals(label, that.label)
                && Objects.equals(schema, that.schema)
                && Objects.equals(type, that.type)
                && Objects.equals(acceptType, that.acceptType)
                && direction == that.direction
                && Objects.equals(maxConnections, that.maxConnections)
                && Objects.equals(shortDescription, that.shortDescription)
                && Objects.equals(ui, that.ui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, schema, type, acceptType, direction, required, minConnections, maxConnections, shortDescription, ui);
    }

    @Override
    public String toString() {
        return "PortDefinition{id='" + id + "', label='" + label + "', direction=" + direction + ", schema='" + schema + "'}";
    }
}
