/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireType;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Studio canvas pipelines and layout helpers for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderCanvas {

    private static final int CANVAS_LAYOUT_X = 80;
    private static final int CANVAS_LAYOUT_Y = 80;
    private static final int CANVAS_LAYOUT_COL_WIDTH = 360;

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;
    private final WorkflowBuilderNodes nodes;

    public WorkflowBuilderCanvas(
            WorkflowBuilderState state, WorkflowBuilder owner, WorkflowBuilderNodes nodes) {
        this.state = state;
        this.owner = owner;
        this.nodes = nodes;
    }

    /** Leaf agent canvas: START → AGENT (inline local LLM) → END. */
    public WorkflowBuilder localAgentCanvasPipeline(String workflowId) {
        Objects.requireNonNull(workflowId, "workflowId is required");
        return nodes.startNodeWithMessageInput("start")
                .addNode(NodeDefinition.builder()
                        .id("agent")
                        .type(NodeType.AGENT.name())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId(workflowId)
                                .version("1.0.0")
                                .build())
                        .executionKind(ExecutionKind.ACTIVITY)
                        .executionModel(ExecutionModel.INLINE)
                        .addPort(WorkflowBuilderPorts.messagePort("in", PortDirection.INPUT))
                        .addPort(WorkflowBuilderPorts.pluginPort("capabilities", PortWireType.CAPABILITIES, PortDirection.INPUT))
                        .addPort(WorkflowBuilderPorts.pluginPort("agentPlug", PortWireType.AGENT_PLUG, PortDirection.INPUT))
                        .addPort(WorkflowBuilderPorts.messagePort("out", PortDirection.OUTPUT))
                        .build())
                .endNode("end")
                .connect("start", "out", "agent", "in")
                .connect("agent", "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout("agent", 1)
                .nodeCanvasLayout("end", 2);
    }

    /** Canonical Studio canvas: START → AGENT (self workflow) → END with message input mapping. */
    public WorkflowBuilder agentCanvasPipeline(String workflowId) {
        return nodes.startNodeWithMessageInput("start")
                .agentNode("agent", WorkflowReferenceDefinition.builder()
                        .workflowId(workflowId)
                        .version("1.0.0")
                        .build())
                .endNode("end")
                .connect("start", "out", "agent", "in")
                .connect("agent", "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout("agent", 1)
                .nodeCanvasLayout("end", 2);
    }

    /** Studio canvas position ({@code configuration.designer.position}). */
    public WorkflowBuilder nodeCanvasLayout(String nodeId, int columnIndex) {
        Objects.requireNonNull(nodeId, "nodeId is required");
        for (int i = 0; i < state.nodes.size(); i++) {
            NodeDefinition node = state.nodes.get(i);
            if (!node.getId().equals(nodeId)) {
                continue;
            }
            java.util.Map<String, Object> configuration = new java.util.LinkedHashMap<>(node.getConfiguration());
            configuration.put("designer", designerLayout(columnIndex));
            state.nodes.set(i, WorkflowBuilderPorts.nodeWithConfiguration(node, configuration));
            return owner;
        }
        throw new IllegalArgumentException("unknown node id for canvas layout: " + nodeId);
    }

    /** Merges entries into an existing node's {@code configuration} map. */
    public WorkflowBuilder putNodeConfiguration(String nodeId, java.util.Map<String, Object> configuration) {
        Objects.requireNonNull(nodeId, "nodeId is required");
        Objects.requireNonNull(configuration, "configuration is required");
        for (int i = 0; i < state.nodes.size(); i++) {
            NodeDefinition node = state.nodes.get(i);
            if (!node.getId().equals(nodeId)) {
                continue;
            }
            java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>();
            if (node.getConfiguration() != null) {
                merged.putAll(node.getConfiguration());
            }
            merged.putAll(configuration);
            state.nodes.set(i, WorkflowBuilderPorts.nodeWithConfiguration(node, merged));
            return owner;
        }
        throw new IllegalArgumentException("unknown node id for configuration: " + nodeId);
    }

    private static java.util.Map<String, Object> designerLayout(int columnIndex) {
        return java.util.Map.of(
                "position",
                java.util.Map.of("x", CANVAS_LAYOUT_X + columnIndex * CANVAS_LAYOUT_COL_WIDTH, "y", CANVAS_LAYOUT_Y));
    }
}
