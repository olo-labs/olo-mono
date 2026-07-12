/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.RuntimeDelegationDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Jackson builder and fluent factory for {@link WorkflowDefinition}.
 *
 * <p>Identity/orchestration methods live on {@link WorkflowDefinitionBuilderBase}; graph, catalog,
 * and build-time runtime normalization live here.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class WorkflowDefinitionBuilder extends WorkflowDefinitionBuilderBase {

    public WorkflowDefinitionBuilder nodes(List<NodeDefinition> nodes) {
        this.nodes = nodes;
        return this;
    }

    public WorkflowDefinitionBuilder addNode(NodeDefinition node) {
        if (this.nodes == null) {
            this.nodes = new ArrayList<>();
        }
        this.nodes.add(node);
        return this;
    }

    public WorkflowDefinitionBuilder edges(List<EdgeDefinition> edges) {
        this.edges = edges;
        return this;
    }

    public WorkflowDefinitionBuilder addEdge(EdgeDefinition edge) {
        if (this.edges == null) {
            this.edges = new ArrayList<>();
        }
        this.edges.add(edge);
        return this;
    }

    public WorkflowDefinitionBuilder inputs(Map<String, WorkflowInputDefinition> inputs) {
        this.inputs = inputs;
        return this;
    }

    public WorkflowDefinitionBuilder putInput(String name, WorkflowInputDefinition input) {
        if (this.inputs == null) {
            this.inputs = new LinkedHashMap<>();
        }
        this.inputs.put(name, input);
        return this;
    }

    public WorkflowDefinitionBuilder state(Map<String, StateFieldDefinition> state) {
        this.state = state;
        return this;
    }

    public WorkflowDefinitionBuilder putState(String name, StateFieldDefinition field) {
        if (this.state == null) {
            this.state = new LinkedHashMap<>();
        }
        this.state.put(name, field);
        return this;
    }

    public WorkflowDefinitionBuilder parameters(Map<String, WorkflowParameterDefinition> parameters) {
        this.parameters = parameters;
        return this;
    }

    public WorkflowDefinitionBuilder putParameter(String name, WorkflowParameterDefinition parameter) {
        if (this.parameters == null) {
            this.parameters = new LinkedHashMap<>();
        }
        this.parameters.put(name, parameter);
        return this;
    }

    /** @deprecated use {@link #putInput(String, WorkflowInputDefinition)} */
    @Deprecated
    public WorkflowDefinitionBuilder variables(List<VariableDefinition> variables) {
        this.variables = variables;
        return this;
    }

    public WorkflowDefinitionBuilder modelProviders(List<ModelProviderDefinition> modelProviders) {
        this.modelProviders = modelProviders;
        return this;
    }

    public WorkflowDefinitionBuilder modelRouting(List<ModelRoutingDefinition> modelRouting) {
        this.modelRouting = modelRouting;
        return this;
    }

    public WorkflowDefinitionBuilder extensions(List<ExtensionDefinition> extensions) {
        this.extensions = extensions;
        return this;
    }

    public WorkflowDefinitionBuilder metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public WorkflowDefinitionBuilder capability(CapabilityDefinition capability) {
        this.capability = capability;
        return this;
    }

    public WorkflowDefinitionBuilder runtime(WorkflowRuntimeDefinition runtime) {
        this.runtime = runtime;
        return this;
    }

    public WorkflowDefinitionBuilder runtimeBinding(RuntimeBindingDefinition runtimeBinding) {
        this.runtimeBinding = runtimeBinding;
        return this;
    }

    public WorkflowDefinitionBuilder tools(List<ToolDefinition> tools) {
        this.tools = tools;
        return this;
    }

    public WorkflowDefinitionBuilder addTool(ToolDefinition tool) {
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }

    public WorkflowDefinitionBuilder agents(List<AgentDefinition> agents) {
        this.agents = agents;
        return this;
    }

    public WorkflowDefinitionBuilder addAgent(AgentDefinition agent) {
        if (this.agents == null) {
            this.agents = new ArrayList<>();
        }
        this.agents.add(agent);
        return this;
    }

    public WorkflowDefinitionBuilder hooks(List<HookDefinition> hooks) {
        this.hooks = hooks;
        return this;
    }

    public WorkflowDefinitionBuilder addHook(HookDefinition hook) {
        if (this.hooks == null) {
            this.hooks = new ArrayList<>();
        }
        this.hooks.add(hook);
        return this;
    }

    public WorkflowDefinition build() {
        Objects.requireNonNull(id, "workflow id is required");
        WorkflowDefinitionBuilderRuntime.normalize(this);
        return new WorkflowDefinition(this);
    }
}
