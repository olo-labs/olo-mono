/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Root declarative artifact for an Open LLM Orchestrator (OLO) workflow graph.
 * Serializable to JSON/YAML; contains no execution or runtime state.
 */
@JsonDeserialize(builder = WorkflowDefinitionBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "id", "enabled", "isDefault", "label", "role", "shortDescription", "emoji", "designer", "queue",
    "workflowType", "runAgain", "longDescription", "isExternalWorkflow", "isChildWorkflow",
    "childWorkflows", "availableAgents", "version", "capability", "runtime", "runtimeBinding",
    "inputs", "state", "parameters", "nodes", "edges", "variables", "tools", "agents", "hooks",
    "modelProviders", "modelRouting", "extensions", "metadata"
})
public final class WorkflowDefinition {

    private final WorkflowDefinitionFields fields;

    WorkflowDefinition(WorkflowDefinitionBuilder builder) {
        this.fields = WorkflowDefinitionFields.from(builder);
    }

    public static WorkflowDefinitionBuilder builder() {
        return new WorkflowDefinitionBuilder();
    }

    public String getId() { return fields.id(); }
    @JsonProperty("enabled") public Boolean isEnabled() { return fields.enabled(); }
    @JsonProperty("isDefault") public Boolean isDefault() { return fields.isDefault(); }
    public String getLabel() { return fields.label(); }
    public String getRole() { return fields.role(); }
    public String getShortDescription() { return fields.shortDescription(); }
    public String getEmoji() { return fields.emoji(); }
    public DesignerDefinition getDesigner() { return fields.designer(); }
    public String getQueue() { return fields.queue(); }
    public String getWorkflowType() { return fields.workflowType(); }
    @JsonProperty("runAgain") public Boolean isRunAgain() { return fields.runAgain(); }
    public String getLongDescription() { return fields.longDescription(); }
    @JsonProperty("isExternalWorkflow") public Boolean isExternalWorkflow() { return fields.isExternalWorkflow(); }
    @JsonProperty("isChildWorkflow") public Boolean isChildWorkflow() { return fields.isChildWorkflow(); }
    public List<ChildWorkflowDefinition> getChildWorkflows() { return fields.childWorkflows(); }
    public List<AgentReferenceDefinition> getAvailableAgents() { return fields.availableAgents(); }
    @JsonIgnore public List<String> getAvailableAgentIds() {
        return fields.availableAgents().stream().map(AgentReferenceDefinition::getId).toList();
    }
    public String getVersion() { return fields.version(); }
    public List<NodeDefinition> getNodes() { return fields.nodes(); }
    public List<EdgeDefinition> getEdges() { return fields.edges(); }
    public Map<String, WorkflowInputDefinition> getInputs() { return fields.inputs(); }
    public Map<String, StateFieldDefinition> getState() { return fields.state(); }
    public Map<String, WorkflowParameterDefinition> getParameters() { return fields.parameters(); }
    @Deprecated public List<VariableDefinition> getVariables() { return fields.variables(); }
    public List<ModelProviderDefinition> getModelProviders() { return fields.modelProviders(); }
    public List<ModelRoutingDefinition> getModelRouting() { return fields.modelRouting(); }
    public List<ExtensionDefinition> getExtensions() { return fields.extensions(); }
    public Map<String, Object> getMetadata() { return fields.metadata(); }
    public CapabilityDefinition getCapability() { return fields.capability(); }
    public WorkflowRuntimeDefinition getRuntime() { return fields.runtime(); }
    public RuntimeBindingDefinition getRuntimeBinding() { return fields.runtimeBinding(); }
    public List<ToolDefinition> getTools() { return fields.tools(); }
    public List<AgentDefinition> getAgents() { return fields.agents(); }
    public List<HookDefinition> getHooks() { return fields.hooks(); }

    public WorkflowDefinition copy() {
        return WorkflowBuilder.from(this).build();
    }

    public WorkflowBuilder toBuilder() {
        return WorkflowBuilder.from(this);
    }

    public static WorkflowDefinition copyOf(WorkflowDefinition source) {
        Objects.requireNonNull(source, "source workflow is required");
        return source.copy();
    }

    @Override
    public boolean equals(Object o) {
        return WorkflowDefinitionEquality.equals(this, o);
    }

    @Override
    public int hashCode() {
        return WorkflowDefinitionEquality.hashCode(this);
    }

    @Override
    public String toString() {
        return WorkflowDefinitionEquality.toString(this);
    }
}
