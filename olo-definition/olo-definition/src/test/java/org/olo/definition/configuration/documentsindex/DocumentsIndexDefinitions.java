/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsindex;

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

/** Programmatic builder for the {@code documents-index} RAG vector ingest pipeline. */
public final class DocumentsIndexDefinitions {

    public static final String PIPELINE_ID = "documents-index";
    public static final String QUEUE = "oloQueue2";
    public static final String RAG_INGEST_NODE_ID = "rag-ingest";
    public static final String RAG_INGEST_TOOL_ID = "olo-core:rag-ingest";
    public static final String VECTOR_STORE_EXTENSION_ID = "pgvector-store";

    private DocumentsIndexDefinitions() {
    }

    public static WorkflowDefinition documentsIndex() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Documents Index")
                .id(PIPELINE_ID)
                .enabled(true)
                .isDefault(false)
                .role("Documents Index")
                .shortDescription("Index uploaded files into the vector store (single rag-ingest step)")
                .emoji("📚")
                .designer(StudioDesignerDefaults.studioToolPipelineDesigner("📚", "rag", "ingest", "documents-index"))
                .queue(QUEUE)
                .workflowType("olo")
                .version("1.0.0")
                .executionModel(ExecutionModel.ACTIVITY)
                .capability(CapabilityDefinition.builder()
                        .name("Documents Index")
                        .description("Ingest uploaded documents into the configured vector store.")
                        .addTag("rag")
                        .addTag("ingest")
                        .addTag("documents")
                        .addInput("input")
                        .addOutput("output")
                        .build())
                .withMessageContract()
                .withStandardReturnVariable()
                .extension(vectorStoreExtension())
                .tool(ragIngestTool())
                .ragIngestCanvasPipeline()
                .metadata("role", PIPELINE_ID)
                .metadata("description", "Dedicated ingest pipeline — creates vector DB entries only")
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

    private static ToolDefinition ragIngestTool() {
        return ToolDefinition.builder()
                .id(RAG_INGEST_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("RAG Ingest")
                        .description("Chunks uploaded files and writes vector index entries")
                        .addExample("Index selected documents for capabilitySource my-knowledge-base")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(RAG_INGEST_TOOL_ID)
                        .build())
                .build();
    }

    static boolean isRagIngestToolNode(NodeDefinition node) {
        if (node == null || !NodeType.TOOL.name().equals(node.getType())) {
            return false;
        }
        Map<String, Object> configuration = node.getConfiguration();
        return configuration != null && RAG_INGEST_TOOL_ID.equals(String.valueOf(configuration.get("toolId")));
    }
}
