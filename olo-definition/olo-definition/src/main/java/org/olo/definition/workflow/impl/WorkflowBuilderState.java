/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinitionBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared mutable state for {@link org.olo.definition.workflow.WorkflowBuilder} and its section helpers.
 */
public final class WorkflowBuilderState {

    final WorkflowDefinitionBuilder delegate = WorkflowDefinition.builder();
    final List<NodeDefinition> nodes = new ArrayList<>();
    final List<EdgeDefinition> edges = new ArrayList<>();
    final Map<String, WorkflowInputDefinition> inputs = new LinkedHashMap<>();
    final Map<String, StateFieldDefinition> state = new LinkedHashMap<>();
    final Map<String, WorkflowParameterDefinition> parameters = new LinkedHashMap<>();
    final List<VariableDefinition> variables = new ArrayList<>();
    final List<ModelProviderDefinition> modelProviders = new ArrayList<>();
    final List<ModelRoutingDefinition> modelRouting = new ArrayList<>();
    final List<ExtensionDefinition> extensions = new ArrayList<>();
    final List<ToolDefinition> tools = new ArrayList<>();
    final List<AgentDefinition> agents = new ArrayList<>();
    final List<HookDefinition> hooks = new ArrayList<>();
    final List<ChildWorkflowDefinition> childWorkflows = new ArrayList<>();
    final List<AgentReferenceDefinition> availableAgents = new ArrayList<>();
    final Map<String, Object> metadata = new LinkedHashMap<>();
    WorkflowRuntimeDefinition.Builder workflowRuntimeBuilder;

    public void initializeCreate(String label, String id) {
        delegate.label(label);
        delegate.id(id);
    }

    public void seedFrom(WorkflowDefinition existing) {
        delegate.id(existing.getId());
        delegate.enabled(existing.isEnabled());
        delegate.isDefault(existing.isDefault());
        delegate.label(existing.getLabel());
        delegate.role(existing.getRole());
        delegate.shortDescription(existing.getShortDescription());
        delegate.emoji(existing.getEmoji());
        if (existing.getDesigner() != null) {
            delegate.designer(existing.getDesigner());
        }
        delegate.queue(existing.getQueue());
        delegate.workflowType(existing.getWorkflowType());
        delegate.runAgain(existing.isRunAgain());
        delegate.longDescription(existing.getLongDescription());
        delegate.isExternalWorkflow(existing.isExternalWorkflow());
        delegate.isChildWorkflow(existing.isChildWorkflow());
        childWorkflows.addAll(existing.getChildWorkflows());
        availableAgents.addAll(existing.getAvailableAgents());
        delegate.version(existing.getVersion());
        nodes.addAll(existing.getNodes());
        edges.addAll(existing.getEdges());
        inputs.putAll(existing.getInputs());
        state.putAll(existing.getState());
        parameters.putAll(existing.getParameters());
        variables.addAll(existing.getVariables());
        modelProviders.addAll(existing.getModelProviders());
        modelRouting.addAll(existing.getModelRouting());
        extensions.addAll(existing.getExtensions());
        tools.addAll(existing.getTools());
        agents.addAll(existing.getAgents());
        hooks.addAll(existing.getHooks());
        metadata.putAll(existing.getMetadata());
        if (existing.getCapability() != null) {
            delegate.capability(existing.getCapability());
        }
        if (existing.getRuntimeBinding() != null) {
            delegate.runtimeBinding(existing.getRuntimeBinding());
        }
        if (existing.getRuntime() != null) {
            workflowRuntimeBuilder = WorkflowRuntimeDefinition.builder()
                    .contractVersion(existing.getRuntime().getContractVersion())
                    .executionModel(existing.getRuntime().getExecutionModel())
                    .capabilities(existing.getRuntime().getCapabilities())
                    .defaultTimeout(existing.getRuntime().getDefaultTimeout())
                    .delegation(existing.getRuntime().getDelegation());
        }
    }

    WorkflowRuntimeDefinition.Builder workflowRuntimeBuilder() {
        if (workflowRuntimeBuilder == null) {
            workflowRuntimeBuilder = WorkflowRuntimeDefinition.builder();
        }
        return workflowRuntimeBuilder;
    }

    public WorkflowDefinition build() {
        delegate.nodes(List.copyOf(nodes));
        delegate.edges(List.copyOf(edges));
        delegate.inputs(Map.copyOf(inputs));
        delegate.state(Map.copyOf(state));
        delegate.parameters(Map.copyOf(parameters));
        delegate.variables(List.copyOf(variables));
        delegate.modelProviders(List.copyOf(modelProviders));
        delegate.modelRouting(List.copyOf(modelRouting));
        delegate.extensions(List.copyOf(extensions));
        delegate.tools(List.copyOf(tools));
        delegate.agents(List.copyOf(agents));
        delegate.hooks(List.copyOf(hooks));
        delegate.childWorkflows(List.copyOf(childWorkflows));
        delegate.availableAgents(List.copyOf(availableAgents));
        delegate.metadata(Map.copyOf(metadata));
        if (workflowRuntimeBuilder != null) {
            delegate.runtime(workflowRuntimeBuilder.build());
        }
        return delegate.build();
    }
}
