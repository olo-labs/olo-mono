package org.olo.worker.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Root worker deployment configuration: server binding, workflow scan paths, and extension metadata.
 * Serializable to JSON/YAML; does not embed workflow graph definitions.
 */
@JsonDeserialize(builder = WorkerConfiguration.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "id",
    "name",
    "server",
    "workflowDefinitions",
    "temporal",
    "cache",
    "input",
    "metadata"
})
public final class WorkerConfiguration {

    private final String id;
    private final String name;
    private final ServerSettings server;
    private final WorkflowDefinitionsSettings workflowDefinitions;
    private final TemporalSettings temporal;
    private final CacheSettings cache;
    private final InputSettings input;
    private final Map<String, Object> metadata;

    private WorkerConfiguration(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.server = builder.server;
        this.workflowDefinitions = builder.workflowDefinitions;
        this.temporal = builder.temporal;
        this.cache = builder.cache;
        this.input = builder.input;
        this.metadata = builder.metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("server")
    public ServerSettings getServer() {
        return server;
    }

    @JsonProperty("workflowDefinitions")
    public WorkflowDefinitionsSettings getWorkflowDefinitions() {
        return workflowDefinitions;
    }

    @JsonProperty("temporal")
    public TemporalSettings getTemporal() {
        return temporal;
    }

    @JsonProperty("cache")
    public CacheSettings getCache() {
        return cache;
    }

    @JsonProperty("input")
    public InputSettings getInput() {
        return input;
    }

    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkerConfiguration that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(server, that.server)
                && Objects.equals(workflowDefinitions, that.workflowDefinitions)
                && Objects.equals(temporal, that.temporal)
                && Objects.equals(cache, that.cache)
                && Objects.equals(input, that.input)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, server, workflowDefinitions, temporal, cache, input, metadata);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String id;
        private String name;
        private ServerSettings server;
        private WorkflowDefinitionsSettings workflowDefinitions;
        private TemporalSettings temporal;
        private CacheSettings cache;
        private InputSettings input;
        private Map<String, Object> metadata;

        @JsonProperty("id")
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("name")
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @JsonProperty("server")
        public Builder server(ServerSettings server) {
            this.server = server;
            return this;
        }

        @JsonProperty("workflowDefinitions")
        public Builder workflowDefinitions(WorkflowDefinitionsSettings workflowDefinitions) {
            this.workflowDefinitions = workflowDefinitions;
            return this;
        }

        @JsonProperty("temporal")
        public Builder temporal(TemporalSettings temporal) {
            this.temporal = temporal;
            return this;
        }

        @JsonProperty("cache")
        public Builder cache(CacheSettings cache) {
            this.cache = cache;
            return this;
        }

        @JsonProperty("input")
        public Builder input(InputSettings input) {
            this.input = input;
            return this;
        }

        @JsonProperty("metadata")
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public WorkerConfiguration build() {
            return new WorkerConfiguration(this);
        }
    }
}
