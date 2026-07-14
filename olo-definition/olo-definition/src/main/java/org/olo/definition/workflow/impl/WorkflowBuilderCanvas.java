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
import org.olo.definition.preset.WorkflowConversationPluginSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.Map;
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

    /** Dedicated RAG ingest canvas: START → rag-ingest TOOL → END (no human step). */
    public WorkflowBuilder ragIngestCanvasPipeline() {
        String ingestNodeId = "rag-ingest";
        return nodes.startNodeWithMessageInput("start")
                .canvasToolNode(ingestNodeId, "RAG Ingest")
                .putNodeConfiguration(
                        ingestNodeId,
                        Map.of(
                                "toolId", "olo-core:rag-ingest",
                                "extensionRef", "pgvector-store",
                                "vectorTable", "documents",
                                "driver", "qdrant",
                                "connectionRef", "http://localhost:46333",
                                "collection", "documents",
                                "vectorSize", 384,
                                "distance", "Cosine",
                                "chunkSize", 512))
                .endNode("end")
                .connect("start", "out", ingestNodeId, "in")
                .connect(ingestNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(ingestNodeId, 1)
                .nodeCanvasLayout("end", 2);
    }

    /** Leaf agent canvas: START → conversation-load → AGENT (inline local LLM) → conversation-store → END. */
    public WorkflowBuilder localAgentCanvasPipeline(String workflowId) {
        Objects.requireNonNull(workflowId, "workflowId is required");
        String loadNodeId = WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID;
        String storeNodeId = WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID;
        owner.withConversationVariables();
        WorkflowConversationPluginSupport.registerConversationTools(owner);
        return nodes.startNodeWithMessageInput("start")
                .toolNode(loadNodeId)
                .putNodeConfiguration(loadNodeId, Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_LOAD_TOOL_ID))
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
                .toolNode(storeNodeId)
                .putNodeConfiguration(storeNodeId, Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_STORE_TOOL_ID))
                .endNode("end")
                .connect("start", "out", loadNodeId, "in")
                .connect(loadNodeId, "out", "agent", "in")
                .connect("agent", "out", storeNodeId, "in")
                .connect(storeNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(loadNodeId, 1)
                .nodeCanvasLayout("agent", 2)
                .nodeCanvasLayout(storeNodeId, 3)
                .nodeCanvasLayout("end", 4);
    }

    /** Canonical Studio canvas: START → conversation-load → AGENT (self workflow) → conversation-store → END. */
    public WorkflowBuilder agentCanvasPipeline(String workflowId) {
        String loadNodeId = WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID;
        String storeNodeId = WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID;
        owner.withConversationVariables();
        WorkflowConversationPluginSupport.registerConversationTools(owner);
        return nodes.startNodeWithMessageInput("start")
                .toolNode(loadNodeId)
                .putNodeConfiguration(loadNodeId, Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_LOAD_TOOL_ID))
                .agentNode("agent", WorkflowReferenceDefinition.builder()
                        .workflowId(workflowId)
                        .version("1.0.0")
                        .build())
                .toolNode(storeNodeId)
                .putNodeConfiguration(storeNodeId, Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_STORE_TOOL_ID))
                .endNode("end")
                .connect("start", "out", loadNodeId, "in")
                .connect(loadNodeId, "out", "agent", "in")
                .connect("agent", "out", storeNodeId, "in")
                .connect(storeNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(loadNodeId, 1)
                .nodeCanvasLayout("agent", 2)
                .nodeCanvasLayout(storeNodeId, 3)
                .nodeCanvasLayout("end", 4);
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
