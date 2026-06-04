package io.olo.definition.port;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Declares a named input or output on a node with a schema type (e.g. {@code Stock[]}, {@code String}).
 * Used by {@link io.olo.definition.validation.WorkflowValidator} to check edge wiring before runtime.
 */
@JsonDeserialize(builder = PortDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortDefinition {

    private final String name;
    private final String schema;

    private PortDefinition(Builder builder) {
        this.name = builder.name;
        this.schema = builder.schema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortDefinition that)) {
            return false;
        }
        return Objects.equals(name, that.name) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema);
    }

    @Override
    public String toString() {
        return "PortDefinition{name='" + name + "', schema='" + schema + "'}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String name;
        private String schema;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public PortDefinition build() {
            Objects.requireNonNull(name, "port name is required");
            Objects.requireNonNull(schema, "port schema is required");
            return new PortDefinition(this);
        }
    }
}
