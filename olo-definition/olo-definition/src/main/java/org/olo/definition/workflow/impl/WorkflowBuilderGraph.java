/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Graph edges, connectivity, inputs, state, and parameters for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderGraph {

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;

    public WorkflowBuilderGraph(WorkflowBuilderState state, WorkflowBuilder owner) {
        this.state = state;
        this.owner = owner;
    }

    public WorkflowBuilder addEdge(EdgeDefinition edge) {
        Objects.requireNonNull(edge, "edge is required");
        state.edges.add(edge);
        return owner;
    }

    public WorkflowBuilder replaceEdges(List<EdgeDefinition> replacementEdges) {
        state.edges.clear();
        if (replacementEdges != null) {
            state.edges.addAll(replacementEdges);
        }
        return owner;
    }

    public WorkflowBuilder replaceNodes(List<org.olo.definition.node.NodeDefinition> replacementNodes) {
        state.nodes.clear();
        if (replacementNodes != null) {
            state.nodes.addAll(replacementNodes);
        }
        return owner;
    }

    public WorkflowBuilder connect(String sourceNodeId, String targetNodeId) {
        return connect(sourceNodeId, null, targetNodeId, null);
    }

    public WorkflowBuilder connect(
            String sourceNodeId,
            String sourcePortId,
            String targetNodeId,
            String targetPortId) {
        return addEdge(EdgeDefinition.builder()
                .sourceNodeId(sourceNodeId)
                .sourcePortId(sourcePortId)
                .targetNodeId(targetNodeId)
                .targetPortId(targetPortId)
                .build());
    }

    public WorkflowBuilder input(String name, WorkflowInputDefinition input) {
        Objects.requireNonNull(name, "input name is required");
        Objects.requireNonNull(input, "input is required");
        state.inputs.put(name, input);
        return owner;
    }

    public WorkflowBuilder stateField(String name, StateFieldDefinition field) {
        Objects.requireNonNull(name, "state field name is required");
        Objects.requireNonNull(field, "state field is required");
        state.state.put(name, field);
        return owner;
    }

    public WorkflowBuilder parameter(String name, WorkflowParameterDefinition parameter) {
        Objects.requireNonNull(name, "parameter name is required");
        Objects.requireNonNull(parameter, "parameter is required");
        state.parameters.put(name, parameter);
        return owner;
    }

    /** @deprecated use {@link #input(String, WorkflowInputDefinition)} */
    @Deprecated
    public WorkflowBuilder variable(VariableDefinition variable) {
        Objects.requireNonNull(variable, "variable is required");
        state.variables.add(variable);
        return owner;
    }
}
