/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.RuntimeDelegationDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.impl.WorkflowBuilderCanvas;
import org.olo.definition.workflow.impl.WorkflowBuilderCore;
import org.olo.definition.workflow.impl.WorkflowBuilderGraph;
import org.olo.definition.workflow.impl.WorkflowBuilderInfrastructure;
import org.olo.definition.workflow.impl.WorkflowBuilderNodes;
import org.olo.definition.workflow.impl.WorkflowBuilderPorts;
import org.olo.definition.workflow.impl.WorkflowBuilderRuntime;
import org.olo.definition.workflow.impl.WorkflowBuilderState;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for {@link WorkflowDefinition}, including dynamic extension of existing workflows.
 * Delegates to {@code org.olo.definition.workflow.impl} section helpers sharing {@link WorkflowBuilderState}.
 */
public final class WorkflowBuilder {

    public static final String RETURN_VARIABLE_METADATA_KEY =
            WorkflowBuilderInfrastructure.RETURN_VARIABLE_METADATA_KEY;
    public static final String RETURN_VARIABLE_NAME = WorkflowBuilderInfrastructure.RETURN_VARIABLE_NAME;
    public static final String RETURN_VARIABLE_ROLE = WorkflowBuilderInfrastructure.RETURN_VARIABLE_ROLE;

    private final WorkflowBuilderState state = new WorkflowBuilderState();
    private final WorkflowBuilderCore core = new WorkflowBuilderCore(state, this);
    private final WorkflowBuilderGraph graph = new WorkflowBuilderGraph(state, this);
    private final WorkflowBuilderNodes nodes = new WorkflowBuilderNodes(state, this, graph);
    private final WorkflowBuilderCanvas canvas = new WorkflowBuilderCanvas(state, this, nodes);
    private final WorkflowBuilderRuntime runtime = new WorkflowBuilderRuntime(state, this, nodes, core);
    private final WorkflowBuilderInfrastructure infrastructure =
            new WorkflowBuilderInfrastructure(state, this, nodes, core);

    private WorkflowBuilder() {
    }

    /** Starts a new workflow; {@code label} is the display name and seeds {@code id} when not set explicitly. */
    public static WorkflowBuilder create(String label) {
        Objects.requireNonNull(label, "label is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.state.initializeCreate(label, WorkflowBuilderCore.slugify(label));
        return builder;
    }

    /** Creates a builder seeded from an existing workflow (copy-on-write for nodes, edges, and related lists). */
    public static WorkflowBuilder from(WorkflowDefinition existing) {
        Objects.requireNonNull(existing, "existing workflow is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.state.seedFrom(existing);
        return builder;
    }

    // --- core: identity, metadata, version, queue ---
    public WorkflowBuilder id(String id) { return core.id(id); }
    public WorkflowBuilder enabled(Boolean enabled) { return core.enabled(enabled); }
    public WorkflowBuilder isDefault(Boolean isDefault) { return core.isDefault(isDefault); }
    public WorkflowBuilder label(String label) { return core.label(label); }
    public WorkflowBuilder role(String role) { return core.role(role); }
    public WorkflowBuilder shortDescription(String shortDescription) { return core.shortDescription(shortDescription); }
    public WorkflowBuilder emoji(String emoji) { return core.emoji(emoji); }
    public WorkflowBuilder queue(String queue) { return core.queue(queue); }
    public WorkflowBuilder workflowType(String workflowType) { return core.workflowType(workflowType); }
    public WorkflowBuilder runAgain(Boolean runAgain) { return core.runAgain(runAgain); }
    public WorkflowBuilder longDescription(String longDescription) { return core.longDescription(longDescription); }
    public WorkflowBuilder isExternalWorkflow(Boolean isExternalWorkflow) { return core.isExternalWorkflow(isExternalWorkflow); }
    public WorkflowBuilder isChildWorkflow(Boolean isChildWorkflow) { return core.isChildWorkflow(isChildWorkflow); }
    public WorkflowBuilder childWorkflow(ChildWorkflowDefinition childWorkflow) { return core.childWorkflow(childWorkflow); }
    public WorkflowBuilder childWorkflowRef(String workflowId) { return core.childWorkflowRef(workflowId); }
    public WorkflowBuilder version(String version) { return core.version(version); }
    public WorkflowBuilder metadata(String key, Object value) { return core.metadata(key, value); }
    public WorkflowBuilder metadata(Map<String, Object> metadata) { return core.metadata(metadata); }
    public WorkflowBuilder designer(DesignerDefinition designer) { return core.designer(designer); }
    public WorkflowBuilder extension(ExtensionDefinition extension) { return core.extension(extension); }
    public WorkflowBuilder tool(ToolDefinition tool) { return core.tool(tool); }
    public WorkflowBuilder agent(AgentDefinition agent) { return core.agent(agent); }
    public WorkflowBuilder hook(HookDefinition hook) { return core.hook(hook); }

    // --- nodes: graph nodes, edges, inputs ---
    public WorkflowBuilder startNode(String id) { return nodes.startNode(id); }
    public WorkflowBuilder startNodeWithMessageInput(String id) { return nodes.startNodeWithMessageInput(id); }
    public WorkflowBuilder endNode(String id) { return nodes.endNode(id); }
    @Deprecated public WorkflowBuilder inputNode(String id) { return nodes.inputNode(id); }
    @Deprecated public WorkflowBuilder outputNode(String id) { return nodes.outputNode(id); }
    public WorkflowBuilder modelNode(String id) { return nodes.modelNode(id); }
    public WorkflowBuilder modelNode(String id, String subtype) { return nodes.modelNode(id, subtype); }
    public WorkflowBuilder toolNode(String id) { return nodes.toolNode(id); }
    public WorkflowBuilder canvasToolNode(String id) { return nodes.canvasToolNode(id); }
    public WorkflowBuilder canvasChildAgentPluginNode(String id, String childWorkflowId, String label) {
        return nodes.canvasChildAgentPluginNode(id, childWorkflowId, label);
    }
    public WorkflowBuilder vectorSearchNode(String id) { return nodes.vectorSearchNode(id); }
    public WorkflowBuilder agentNode(String id, WorkflowReferenceDefinition workflow) { return nodes.agentNode(id, workflow); }
    public WorkflowBuilder agentNode(String id, String subtype, WorkflowReferenceDefinition workflow) {
        return nodes.agentNode(id, subtype, workflow);
    }
    public WorkflowBuilder humanNode(String id, HumanApprovalDefinition approval) { return nodes.humanNode(id, approval); }
    public WorkflowBuilder humanNode(String id, String subtype, HumanApprovalDefinition approval) {
        return nodes.humanNode(id, subtype, approval);
    }
    public WorkflowBuilder addNode(NodeDefinition node) { return nodes.addNode(node); }
    public WorkflowBuilder addEdge(EdgeDefinition edge) { return nodes.addEdge(edge); }
    public WorkflowBuilder replaceEdges(List<EdgeDefinition> replacementEdges) { return nodes.replaceEdges(replacementEdges); }
    public WorkflowBuilder replaceNodes(List<NodeDefinition> replacementNodes) { return nodes.replaceNodes(replacementNodes); }
    public WorkflowBuilder connect(String sourceNodeId, String targetNodeId) { return nodes.connect(sourceNodeId, targetNodeId); }
    public WorkflowBuilder connect(String sourceNodeId, String sourcePortId, String targetNodeId, String targetPortId) {
        return nodes.connect(sourceNodeId, sourcePortId, targetNodeId, targetPortId);
    }
    public WorkflowBuilder input(String name, WorkflowInputDefinition input) { return nodes.input(name, input); }
    public WorkflowBuilder stateField(String name, StateFieldDefinition field) { return nodes.stateField(name, field); }
    public WorkflowBuilder parameter(String name, WorkflowParameterDefinition parameter) { return nodes.parameter(name, parameter); }
    @Deprecated public WorkflowBuilder variable(VariableDefinition variable) { return nodes.variable(variable); }

    // --- canvas: pipelines, layout, ports ---
    public WorkflowBuilder localAgentCanvasPipeline(String workflowId) { return canvas.localAgentCanvasPipeline(workflowId); }
    public WorkflowBuilder agentCanvasPipeline(String workflowId) { return canvas.agentCanvasPipeline(workflowId); }
    public WorkflowBuilder nodeCanvasLayout(String nodeId, int columnIndex) { return canvas.nodeCanvasLayout(nodeId, columnIndex); }
    public WorkflowBuilder putNodeConfiguration(String nodeId, Map<String, Object> configuration) {
        return canvas.putNodeConfiguration(nodeId, configuration);
    }
    public static PortDefinition messagePort(String id, PortDirection direction) { return WorkflowBuilderPorts.messagePort(id, direction); }
    public static PortDefinition capabilitiesPort(PortDirection direction) { return WorkflowBuilderPorts.capabilitiesPort(direction); }
    public static PortDefinition agentPlugPort(PortDirection direction) { return WorkflowBuilderPorts.agentPlugPort(direction); }

    // --- infrastructure: models, message contract, planner context ---
    public WorkflowBuilder modelProvider(ModelProviderDefinition provider) { return infrastructure.modelProvider(provider); }
    public WorkflowBuilder modelRouting(ModelRoutingDefinition routing) { return infrastructure.modelRouting(routing); }
    public WorkflowBuilder withMessageInput() { return infrastructure.withMessageInput(); }
    public WorkflowBuilder withMessageVariable() { return infrastructure.withMessageVariable(); }
    public WorkflowBuilder withMessageContract() { return infrastructure.withMessageContract(); }
    public WorkflowBuilder defaultLocalModelInfrastructure() { return infrastructure.defaultLocalModelInfrastructure(); }
    public WorkflowBuilder presetPlannerContext(String presetId) { return infrastructure.presetPlannerContext(presetId); }
    public WorkflowBuilder withStandardReturnVariable() { return infrastructure.withStandardReturnVariable(); }

    // --- runtime: capability, execution, delegation ---
    public WorkflowBuilder capability(CapabilityDefinition capability) { return runtime.capability(capability); }
    public WorkflowBuilder runtime(WorkflowRuntimeDefinition runtimeDefinition) { return runtime.runtime(runtimeDefinition); }
    public WorkflowBuilder executionModel(ExecutionModel executionModel) { return runtime.executionModel(executionModel); }
    public WorkflowBuilder defaultTimeout(String defaultTimeout) { return runtime.defaultTimeout(defaultTimeout); }
    public WorkflowBuilder agentWorkflowRuntime() { return runtime.agentWorkflowRuntime(); }
    public WorkflowBuilder baselineAgentParameters() { return runtime.baselineAgentParameters(); }
    public WorkflowBuilder agentParameters() { return runtime.agentParameters(); }
    public WorkflowBuilder agentParameters(String presetId) { return runtime.agentParameters(presetId); }
    public WorkflowBuilder agentPlannerMetadata() { return runtime.agentPlannerMetadata(); }
    public WorkflowBuilder agentPlannerContext() { return runtime.agentPlannerContext(); }
    public WorkflowBuilder availableAgent(String agentWorkflowId) { return runtime.availableAgent(agentWorkflowId); }
    public WorkflowBuilder availableAgent(AgentReferenceDefinition agentReference) { return runtime.availableAgent(agentReference); }
    public WorkflowBuilder agentAvailableAgents() { return runtime.agentAvailableAgents(); }
    public WorkflowBuilder delegation(RuntimeDelegationDefinition delegation) { return runtime.delegation(delegation); }
    public WorkflowBuilder agentDelegation() { return runtime.agentDelegation(); }
    public WorkflowBuilder debuggable() { return runtime.debuggable(); }
    public WorkflowBuilder replayable() { return runtime.replayable(); }
    public WorkflowBuilder addRuntimeCapability(RuntimeCapability capability) { return runtime.addRuntimeCapability(capability); }
    public WorkflowBuilder runtimeBinding(RuntimeBindingDefinition runtimeBinding) { return runtime.runtimeBinding(runtimeBinding); }

    public WorkflowDefinition build() {
        return state.build();
    }
}
