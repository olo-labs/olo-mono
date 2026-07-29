/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsdelete;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Map;

/** Programmatic builder for the {@code documents-delete} RAG vector delete pipeline. */
public final class DocumentsDeleteDefinitions {

    public static final String PIPELINE_ID = "documents-delete";
    public static final String QUEUE = "oloQueue2";
    public static final String RAG_DELETE_NODE_ID = "rag-delete";
    public static final String RAG_DELETE_TOOL_ID = "olo-core:rag-delete";
    public static final String VECTOR_STORE_EXTENSION_ID = "pgvector-store";

    private DocumentsDeleteDefinitions() {
    }

    public static WorkflowDefinition documentsDelete() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Documents Delete")
                .id(PIPELINE_ID)
                .enabled(true)
                .isDefault(false)
                .role("Documents Delete")
                .shortDescription("Delete tokenized knowledge entries from the vector store (single rag-delete step)")
                .emoji("X")
                .designer(StudioDesignerDefaults.studioToolPipelineDesigner("X", "rag", "delete", "documents-delete"))
                .queue(QUEUE)
                .workflowType("olo")
                .version("1.0.0")
                .executionModel(ExecutionModel.ACTIVITY)
                .capability(CapabilityDefinition.builder()
                        .name("Documents Delete")
                        .description("Remove tokenized knowledge entries from the configured vector store.")
                        .addTag("rag")
                        .addTag("delete")
                        .addTag("documents")
                        .addInput("input")
                        .addOutput("output")
                        .build())
                .withMessageContract()
                .withStandardReturnVariable()
                .extension(vectorStoreExtension())
                .tool(ragDeleteTool())
                .startNodeWithMessageInput("start")
                .canvasToolNode(RAG_DELETE_NODE_ID, "RAG Delete")
                .putNodeConfiguration(RAG_DELETE_NODE_ID, Map.of(
                        "toolId", RAG_DELETE_TOOL_ID,
                        "extensionRef", VECTOR_STORE_EXTENSION_ID,
                        "vectorTable", "${env:OLO_VECTOR_STORE_TABLE}",
                        "driver", "${env:OLO_VECTOR_STORE_DRIVER}",
                        "connectionRef", "${env:OLO_VECTOR_STORE_URL}",
                        "collection", "${env:OLO_VECTOR_STORE_COLLECTION}",
                        "vectorSize", "${env:OLO_VECTOR_STORE_VECTOR_SIZE}",
                        "distance", "${env:OLO_VECTOR_STORE_DISTANCE}"))
                .endNode("end")
                .connect("start", "out", RAG_DELETE_NODE_ID, "in")
                .connect(RAG_DELETE_NODE_ID, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(RAG_DELETE_NODE_ID, 1)
                .nodeCanvasLayout("end", 2)
                .metadata("role", PIPELINE_ID)
                .metadata("description", "Dedicated delete pipeline - removes vector DB entries only")
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    private static ExtensionDefinition vectorStoreExtension() {
        return ExtensionDefinition.builder()
                .id(VECTOR_STORE_EXTENSION_ID)
                .type("VECTOR_STORE")
                .configuration(Map.of(
                        "driver", "${env:OLO_VECTOR_STORE_DRIVER}",
                        "connectionRef", "${env:OLO_VECTOR_STORE_URL}",
                        "table", "${env:OLO_VECTOR_STORE_TABLE}",
                        "collection", "${env:OLO_VECTOR_STORE_COLLECTION}",
                        "vectorSize", "${env:OLO_VECTOR_STORE_VECTOR_SIZE}",
                        "distance", "${env:OLO_VECTOR_STORE_DISTANCE}"))
                .build();
    }

    private static ToolDefinition ragDeleteTool() {
        return ToolDefinition.builder()
                .id(RAG_DELETE_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("RAG Delete")
                        .description("Deletes tokenized knowledge entries from the vector store")
                        .addExample("Delete knowledge source my-knowledge-base")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(RAG_DELETE_TOOL_ID)
                        .build())
                .build();
    }

    static boolean isRagDeleteToolNode(NodeDefinition node) {
        if (node == null || !NodeType.TOOL.name().equals(node.getType())) {
            return false;
        }
        Map<String, Object> configuration = node.getConfiguration();
        return configuration != null && RAG_DELETE_TOOL_ID.equals(String.valueOf(configuration.get("toolId")));
    }
}
