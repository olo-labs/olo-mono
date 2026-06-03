package io.olo.definition.workflow;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.extension.ExtensionDefinition;
import io.olo.definition.model.ModelProviderDefinition;
import io.olo.definition.model.ModelRoutingDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.variable.VariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for {@link WorkflowDefinition}, including dynamic extension of existing workflows.
 */
public final class WorkflowBuilder {

    private final WorkflowDefinition.Builder delegate = WorkflowDefinition.builder();
    private final List<NodeDefinition> nodes = new ArrayList<>();
    private final List<EdgeDefinition> edges = new ArrayList<>();
    private final List<VariableDefinition> variables = new ArrayList<>();
    private final List<ModelProviderDefinition> modelProviders = new ArrayList<>();
    private final List<ModelRoutingDefinition> modelRouting = new ArrayList<>();
    private final List<ExtensionDefinition> extensions = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    private WorkflowBuilder() {
    }

    /**
     * Starts a new workflow; {@code name} is used as display name and to derive {@code id} if not set explicitly.
     */
    public static WorkflowBuilder create(String name) {
        Objects.requireNonNull(name, "name is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.delegate.name(name);
        builder.delegate.id(slugify(name));
        return builder;
    }

    /**
     * Creates a builder seeded from an existing workflow (copy-on-write for nodes, edges, and related lists).
     */
    public static WorkflowBuilder from(WorkflowDefinition existing) {
        Objects.requireNonNull(existing, "existing workflow is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.delegate.id(existing.getId());
        builder.delegate.name(existing.getName());
        builder.delegate.version(existing.getVersion());
        builder.nodes.addAll(existing.getNodes());
        builder.edges.addAll(existing.getEdges());
        builder.variables.addAll(existing.getVariables());
        builder.modelProviders.addAll(existing.getModelProviders());
        builder.modelRouting.addAll(existing.getModelRouting());
        builder.extensions.addAll(existing.getExtensions());
        builder.metadata.putAll(existing.getMetadata());
        return builder;
    }

    public WorkflowBuilder id(String id) {
        delegate.id(id);
        return this;
    }

    public WorkflowBuilder name(String name) {
        delegate.name(name);
        return this;
    }

    public WorkflowBuilder version(String version) {
        delegate.version(version);
        return this;
    }

    public WorkflowBuilder inputNode(String id) {
        return addNode(NodeDefinition.builder().id(id).type(NodeType.INPUT).build());
    }

    public WorkflowBuilder outputNode(String id) {
        return addNode(NodeDefinition.builder().id(id).type(NodeType.OUTPUT).build());
    }

    public WorkflowBuilder modelNode(String id) {
        return modelNode(id, null);
    }

    public WorkflowBuilder modelNode(String id, String subtype) {
        NodeDefinition.Builder node = NodeDefinition.builder().id(id).type(NodeType.MODEL);
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder toolNode(String id) {
        return addNode(NodeDefinition.builder().id(id).type(NodeType.TOOL).build());
    }

    public WorkflowBuilder vectorSearchNode(String id) {
        return addNode(NodeDefinition.builder().id(id).type(NodeType.VECTOR_SEARCH).build());
    }

    public WorkflowBuilder addNode(NodeDefinition node) {
        Objects.requireNonNull(node, "node is required");
        ensureUniqueNodeId(node.getId());
        nodes.add(node);
        return this;
    }

    public WorkflowBuilder addEdge(EdgeDefinition edge) {
        Objects.requireNonNull(edge, "edge is required");
        edges.add(edge);
        return this;
    }

    public WorkflowBuilder connect(String sourceNodeId, String targetNodeId) {
        return connect(sourceNodeId, null, targetNodeId, null);
    }

    public WorkflowBuilder connect(
            String sourceNodeId,
            String sourcePort,
            String targetNodeId,
            String targetPort) {
        return addEdge(EdgeDefinition.builder()
                .sourceNodeId(sourceNodeId)
                .sourcePort(sourcePort)
                .targetNodeId(targetNodeId)
                .targetPort(targetPort)
                .build());
    }

    public WorkflowBuilder variable(VariableDefinition variable) {
        Objects.requireNonNull(variable, "variable is required");
        variables.add(variable);
        return this;
    }

    public WorkflowBuilder modelProvider(ModelProviderDefinition provider) {
        Objects.requireNonNull(provider, "provider is required");
        modelProviders.add(provider);
        return this;
    }

    public WorkflowBuilder modelRouting(ModelRoutingDefinition routing) {
        Objects.requireNonNull(routing, "routing is required");
        modelRouting.add(routing);
        return this;
    }

    public WorkflowBuilder extension(ExtensionDefinition extension) {
        Objects.requireNonNull(extension, "extension is required");
        extensions.add(extension);
        return this;
    }

    public WorkflowBuilder metadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public WorkflowBuilder metadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public WorkflowDefinition build() {
        delegate.nodes(List.copyOf(nodes));
        delegate.edges(List.copyOf(edges));
        delegate.variables(List.copyOf(variables));
        delegate.modelProviders(List.copyOf(modelProviders));
        delegate.modelRouting(List.copyOf(modelRouting));
        delegate.extensions(List.copyOf(extensions));
        delegate.metadata(Map.copyOf(metadata));
        return delegate.build();
    }

    private void ensureUniqueNodeId(String id) {
        for (NodeDefinition existing : nodes) {
            if (existing.getId().equals(id)) {
                throw new IllegalArgumentException("duplicate node id: " + id);
            }
        }
    }

    private static String slugify(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
