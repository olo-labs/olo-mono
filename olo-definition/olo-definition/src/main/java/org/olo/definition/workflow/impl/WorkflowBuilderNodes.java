/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireType;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Graph node helpers for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderNodes {

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;
    private final WorkflowBuilderGraph graph;

    public WorkflowBuilderNodes(
            WorkflowBuilderState state, WorkflowBuilder owner, WorkflowBuilderGraph graph) {
        this.state = state;
        this.owner = owner;
        this.graph = graph;
    }

    public WorkflowBuilder startNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.START)
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT))
                .build());
    }

    public WorkflowBuilder startNodeWithMessageInput(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.START)
                .addRead("input." + WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT))
                .putConfiguration("inputVariableMappings", List.of(WorkflowPresetInfrastructure.MESSAGE_VARIABLE))
                .build());
    }

    public WorkflowBuilder endNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.END)
                .addPort(WorkflowBuilderPorts.defaultPort("in", "in", PortDirection.INPUT))
                .build());
    }

    @Deprecated
    public WorkflowBuilder inputNode(String id) {
        return startNode(id);
    }

    @Deprecated
    public WorkflowBuilder outputNode(String id) {
        return endNode(id);
    }

    public WorkflowBuilder modelNode(String id) {
        return modelNode(id, null);
    }

    public WorkflowBuilder modelNode(String id, String subtype) {
        NodeDefinitionBuilder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.MODEL)
                .addPort(WorkflowBuilderPorts.defaultPort("in", "in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder toolNode(String id) {
        return canvasToolNode(id);
    }

    public WorkflowBuilder canvasToolNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.TOOL)
                .addPort(WorkflowBuilderPorts.optionalMessagePort("in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT))
                .addPort(WorkflowBuilderPorts.pluginPort("capabilities", PortWireType.CAPABILITIES, PortDirection.OUTPUT))
                .build());
    }

    public WorkflowBuilder canvasChildAgentPluginNode(String id, String childWorkflowId, String label) {
        Objects.requireNonNull(childWorkflowId, "childWorkflowId is required");
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.AGENT)
                .label(label)
                .workflow(WorkflowReferenceDefinition.builder().workflowId(childWorkflowId).version("1.0.0").build())
                .executionKind(ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .addPort(WorkflowBuilderPorts.optionalMessagePort("in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT))
                .addPort(WorkflowBuilderPorts.pluginPort("agentPlug", PortWireType.AGENT_PLUG, PortDirection.OUTPUT))
                .putConfiguration("delegateAgentId", childWorkflowId)
                .build());
    }

    public WorkflowBuilder vectorSearchNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.VECTOR_SEARCH)
                .addPort(WorkflowBuilderPorts.defaultPort("in", "in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT))
                .build());
    }

    public WorkflowBuilder agentNode(String id, WorkflowReferenceDefinition workflow) {
        return agentNode(id, null, workflow);
    }

    public WorkflowBuilder agentNode(String id, String subtype, WorkflowReferenceDefinition workflow) {
        Objects.requireNonNull(workflow, "workflow is required for AGENT nodes");
        NodeDefinitionBuilder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.AGENT)
                .workflow(workflow)
                .executionKind(ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .addPort(WorkflowBuilderPorts.defaultPort("in", "in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.pluginPort("capabilities", PortWireType.CAPABILITIES, PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.pluginPort("agentPlug", PortWireType.AGENT_PLUG, PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder humanNode(String id, HumanApprovalDefinition approval) {
        return humanNode(id, "APPROVAL", approval);
    }

    public WorkflowBuilder humanNode(String id, String subtype, HumanApprovalDefinition approval) {
        Objects.requireNonNull(approval, "approval is required");
        NodeDefinitionBuilder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.HUMAN)
                .approval(approval)
                .executionKind(ExecutionKind.HUMAN_WAIT)
                .addPort(WorkflowBuilderPorts.defaultPort("in", "in", PortDirection.INPUT))
                .addPort(WorkflowBuilderPorts.defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder addNode(NodeDefinition node) {
        Objects.requireNonNull(node, "node is required");
        ensureUniqueNodeId(node.getId());
        state.nodes.add(node);
        return owner;
    }

    // --- delegated graph operations (same fluent surface on WorkflowBuilder) ---

    public WorkflowBuilder addEdge(org.olo.definition.edge.EdgeDefinition edge) {
        return graph.addEdge(edge);
    }

    public WorkflowBuilder replaceEdges(List<org.olo.definition.edge.EdgeDefinition> replacementEdges) {
        return graph.replaceEdges(replacementEdges);
    }

    public WorkflowBuilder replaceNodes(List<NodeDefinition> replacementNodes) {
        return graph.replaceNodes(replacementNodes);
    }

    public WorkflowBuilder connect(String sourceNodeId, String targetNodeId) {
        return graph.connect(sourceNodeId, targetNodeId);
    }

    public WorkflowBuilder connect(
            String sourceNodeId, String sourcePortId, String targetNodeId, String targetPortId) {
        return graph.connect(sourceNodeId, sourcePortId, targetNodeId, targetPortId);
    }

    public WorkflowBuilder input(String name, org.olo.definition.input.WorkflowInputDefinition input) {
        return graph.input(name, input);
    }

    public WorkflowBuilder stateField(String name, org.olo.definition.state.StateFieldDefinition field) {
        return graph.stateField(name, field);
    }

    public WorkflowBuilder parameter(String name, org.olo.definition.parameter.WorkflowParameterDefinition parameter) {
        return graph.parameter(name, parameter);
    }

    @Deprecated
    public WorkflowBuilder variable(org.olo.definition.variable.VariableDefinition variable) {
        return graph.variable(variable);
    }

    void ensureUniqueNodeId(String id) {
        for (NodeDefinition existing : state.nodes) {
            if (existing.getId().equals(id)) {
                throw new IllegalArgumentException("duplicate node id: " + id);
            }
        }
    }
}
