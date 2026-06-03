package io.olo.definition.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.extension.ExtensionDefinition;
import io.olo.definition.model.ModelProviderDefinition;
import io.olo.definition.model.ModelRoutingDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.variable.VariableDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Root declarative artifact for an AI orchestration workflow graph.
 * Serializable to JSON/YAML; contains no execution or runtime state.
 */
@JsonDeserialize(builder = WorkflowDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WorkflowDefinition {

    private final String id;
    private final String name;
    private final String version;
    private final List<NodeDefinition> nodes;
    private final List<EdgeDefinition> edges;
    private final List<VariableDefinition> variables;
    private final List<ModelProviderDefinition> modelProviders;
    private final List<ModelRoutingDefinition> modelRouting;
    private final List<ExtensionDefinition> extensions;
    private final Map<String, Object> metadata;

    private WorkflowDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.version = builder.version;
        this.nodes = builder.nodes == null ? List.of() : List.copyOf(builder.nodes);
        this.edges = builder.edges == null ? List.of() : List.copyOf(builder.edges);
        this.variables = builder.variables == null ? List.of() : List.copyOf(builder.variables);
        this.modelProviders = builder.modelProviders == null ? List.of() : List.copyOf(builder.modelProviders);
        this.modelRouting = builder.modelRouting == null ? List.of() : List.copyOf(builder.modelRouting);
        this.extensions = builder.extensions == null ? List.of() : List.copyOf(builder.extensions);
        this.metadata = builder.metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public List<NodeDefinition> getNodes() {
        return nodes;
    }

    public List<EdgeDefinition> getEdges() {
        return edges;
    }

    public List<VariableDefinition> getVariables() {
        return variables;
    }

    public List<ModelProviderDefinition> getModelProviders() {
        return modelProviders;
    }

    public List<ModelRoutingDefinition> getModelRouting() {
        return modelRouting;
    }

    public List<ExtensionDefinition> getExtensions() {
        return extensions;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowDefinition that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Objects.equals(nodes, that.nodes)
                && Objects.equals(edges, that.edges)
                && Objects.equals(variables, that.variables)
                && Objects.equals(modelProviders, that.modelProviders)
                && Objects.equals(modelRouting, that.modelRouting)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                version,
                nodes,
                edges,
                variables,
                modelProviders,
                modelRouting,
                extensions,
                metadata);
    }

    @Override
    public String toString() {
        return "WorkflowDefinition{id='" + id + "', name='" + name + "', nodes=" + nodes.size() + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String id;
        private String name;
        private String version;
        private List<NodeDefinition> nodes;
        private List<EdgeDefinition> edges;
        private List<VariableDefinition> variables;
        private List<ModelProviderDefinition> modelProviders;
        private List<ModelRoutingDefinition> modelRouting;
        private List<ExtensionDefinition> extensions;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder nodes(List<NodeDefinition> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder addNode(NodeDefinition node) {
            if (this.nodes == null) {
                this.nodes = new java.util.ArrayList<>();
            }
            this.nodes.add(node);
            return this;
        }

        public Builder edges(List<EdgeDefinition> edges) {
            this.edges = edges;
            return this;
        }

        public Builder addEdge(EdgeDefinition edge) {
            if (this.edges == null) {
                this.edges = new java.util.ArrayList<>();
            }
            this.edges.add(edge);
            return this;
        }

        public Builder variables(List<VariableDefinition> variables) {
            this.variables = variables;
            return this;
        }

        public Builder modelProviders(List<ModelProviderDefinition> modelProviders) {
            this.modelProviders = modelProviders;
            return this;
        }

        public Builder modelRouting(List<ModelRoutingDefinition> modelRouting) {
            this.modelRouting = modelRouting;
            return this;
        }

        public Builder extensions(List<ExtensionDefinition> extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public WorkflowDefinition build() {
            Objects.requireNonNull(id, "workflow id is required");
            return new WorkflowDefinition(this);
        }
    }
}
