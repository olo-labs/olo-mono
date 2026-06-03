package io.olo.definition.node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single node in a workflow graph. Type-specific behavior is expressed via
 * {@code type}, optional {@code subtype}, and a flexible {@code configuration} map.
 */
@JsonDeserialize(builder = NodeDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeDefinition {

    private final String id;
    private final String type;
    private final String subtype;
    private final String version;
    private final List<NodeRouterDefinition> routers;
    private final Map<String, Object> configuration;

    private NodeDefinition(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.subtype = builder.subtype;
        this.version = builder.version;
        this.routers = builder.routers == null ? List.of() : List.copyOf(builder.routers);
        this.configuration = builder.configuration == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.configuration));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getVersion() {
        return version;
    }

    public List<NodeRouterDefinition> getRouters() {
        return routers;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeDefinition that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(type, that.type)
                && Objects.equals(subtype, that.subtype)
                && Objects.equals(version, that.version)
                && Objects.equals(routers, that.routers)
                && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, subtype, version, routers, configuration);
    }

    @Override
    public String toString() {
        return "NodeDefinition{id='" + id + "', type='" + type + "'}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String id;
        private String type;
        private String subtype;
        private String version;
        private List<NodeRouterDefinition> routers;
        private Map<String, Object> configuration;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder type(NodeType type) {
            this.type = type == null ? null : type.value();
            return this;
        }

        public Builder subtype(String subtype) {
            this.subtype = subtype;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder routers(List<NodeRouterDefinition> routers) {
            this.routers = routers;
            return this;
        }

        public Builder addRouter(NodeRouterDefinition router) {
            if (this.routers == null) {
                this.routers = new java.util.ArrayList<>();
            }
            this.routers.add(router);
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder putConfiguration(String key, Object value) {
            if (this.configuration == null) {
                this.configuration = new LinkedHashMap<>();
            }
            this.configuration.put(key, value);
            return this;
        }

        public NodeDefinition build() {
            Objects.requireNonNull(id, "node id is required");
            Objects.requireNonNull(type, "node type is required");
            return new NodeDefinition(this);
        }
    }
}
