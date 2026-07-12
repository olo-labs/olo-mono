/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.orchestration.WorkflowOrchestrationDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinitionDeserializer;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.planner.AgentReferenceDefinitionDeserializer;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared mutable state and identity/orchestration fluent methods for {@link WorkflowDefinitionBuilder}.
 *
 * <p>Split across two classes so each stays under 200 lines while Jackson still deserializes into
 * {@link WorkflowDefinitionBuilder}.
 */
class WorkflowDefinitionBuilderBase {

    String id;
    Boolean enabled;
    @JsonProperty("isDefault")
    Boolean isDefault;
    String label;
    String role;
    String shortDescription;
    String emoji;
    DesignerDefinition designer;
    String queue;
    String workflowType;
    Boolean runAgain;
    String longDescription;
    @JsonProperty("isExternalWorkflow")
    Boolean isExternalWorkflow;
    @JsonProperty("isChildWorkflow")
    Boolean isChildWorkflow;
    @JsonDeserialize(contentUsing = ChildWorkflowDefinitionDeserializer.class)
    List<ChildWorkflowDefinition> childWorkflows;
    @JsonDeserialize(contentUsing = AgentReferenceDefinitionDeserializer.class)
    List<AgentReferenceDefinition> availableAgents;
    WorkflowOrchestrationDefinition legacyOrchestration;
    String version;
    List<NodeDefinition> nodes;
    List<EdgeDefinition> edges;
    Map<String, WorkflowInputDefinition> inputs;
    Map<String, StateFieldDefinition> state;
    @JsonDeserialize(contentUsing = WorkflowParameterDefinitionDeserializer.class)
    Map<String, WorkflowParameterDefinition> parameters;
    List<VariableDefinition> variables;
    List<ModelProviderDefinition> modelProviders;
    List<ModelRoutingDefinition> modelRouting;
    List<ExtensionDefinition> extensions;
    Map<String, Object> metadata;
    CapabilityDefinition capability;
    WorkflowRuntimeDefinition runtime;
    RuntimeBindingDefinition runtimeBinding;
    List<ToolDefinition> tools;
    List<AgentDefinition> agents;
    List<HookDefinition> hooks;

    @SuppressWarnings("unchecked")
    protected final WorkflowDefinitionBuilder self() {
        return (WorkflowDefinitionBuilder) this;
    }

    public WorkflowDefinitionBuilder id(String id) {
        this.id = id;
        return self();
    }

    public WorkflowDefinitionBuilder enabled(Boolean enabled) {
        this.enabled = enabled;
        return self();
    }

    public WorkflowDefinitionBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return self();
    }

    @JsonAlias("name")
    public WorkflowDefinitionBuilder label(String label) {
        this.label = label;
        return self();
    }

    public WorkflowDefinitionBuilder role(String role) {
        this.role = role;
        return self();
    }

    public WorkflowDefinitionBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return self();
    }

    public WorkflowDefinitionBuilder emoji(String emoji) {
        this.emoji = emoji;
        return self();
    }

    public WorkflowDefinitionBuilder designer(DesignerDefinition designer) {
        this.designer = designer;
        return self();
    }

    public WorkflowDefinitionBuilder queue(String queue) {
        this.queue = queue;
        return self();
    }

    public WorkflowDefinitionBuilder workflowType(String workflowType) {
        this.workflowType = workflowType;
        return self();
    }

    public WorkflowDefinitionBuilder runAgain(Boolean runAgain) {
        this.runAgain = runAgain;
        return self();
    }

    public WorkflowDefinitionBuilder longDescription(String longDescription) {
        this.longDescription = longDescription;
        return self();
    }

    public WorkflowDefinitionBuilder isExternalWorkflow(Boolean isExternalWorkflow) {
        this.isExternalWorkflow = isExternalWorkflow;
        return self();
    }

    public WorkflowDefinitionBuilder isChildWorkflow(Boolean isChildWorkflow) {
        this.isChildWorkflow = isChildWorkflow;
        return self();
    }

    public WorkflowDefinitionBuilder childWorkflows(List<ChildWorkflowDefinition> childWorkflows) {
        this.childWorkflows = childWorkflows;
        return self();
    }

    public WorkflowDefinitionBuilder addChildWorkflow(ChildWorkflowDefinition childWorkflow) {
        if (this.childWorkflows == null) {
            this.childWorkflows = new ArrayList<>();
        }
        this.childWorkflows.add(childWorkflow);
        return self();
    }

    public WorkflowDefinitionBuilder availableAgents(List<AgentReferenceDefinition> availableAgents) {
        this.availableAgents = availableAgents;
        return self();
    }

    public WorkflowDefinitionBuilder addAvailableAgent(String agentWorkflowId) {
        return addAvailableAgent(AgentReferenceDefinition.of(agentWorkflowId));
    }

    public WorkflowDefinitionBuilder addAvailableAgent(AgentReferenceDefinition agentReference) {
        if (this.availableAgents == null) {
            this.availableAgents = new ArrayList<>();
        }
        this.availableAgents.add(agentReference);
        return self();
    }

    @com.fasterxml.jackson.annotation.JsonSetter("orchestration")
    public WorkflowDefinitionBuilder legacyOrchestration(WorkflowOrchestrationDefinition legacyOrchestration) {
        this.legacyOrchestration = legacyOrchestration;
        return self();
    }

    public WorkflowDefinitionBuilder version(String version) {
        this.version = version;
        return self();
    }
}
