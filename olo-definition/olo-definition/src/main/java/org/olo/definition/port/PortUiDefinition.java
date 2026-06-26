package org.olo.definition.port;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Studio presentation hints for a workflow node port ({@code nodes.*.ports.*.ui}).
 */
@JsonDeserialize(builder = PortUiDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortUiDefinition {

    private final PortUiPosition position;
    private final String color;

    private PortUiDefinition(Builder builder) {
        this.position = builder.position;
        this.color = builder.color;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PortUiDefinition forDirection(PortDirection direction) {
        return builder().position(PortUiPosition.defaultFor(direction)).build();
    }

    public PortUiPosition getPosition() {
        return position;
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortUiDefinition that)) {
            return false;
        }
        return position == that.position && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, color);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private PortUiPosition position;
        private String color;

        public Builder position(PortUiPosition position) {
            this.position = position;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public PortUiDefinition build() {
            return new PortUiDefinition(this);
        }
    }
}
