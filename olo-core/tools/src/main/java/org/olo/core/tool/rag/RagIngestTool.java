/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloExecutionModel;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloStability;
import org.olo.annotation.OloTool;
import org.olo.core.tool.CoreToolIds;
import org.olo.core.tool.ToolArgs;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes uploaded documents into the configured vector store extension (file-json demo driver by default).
 */
@OloTool(
        id = CoreToolIds.RAG_INGEST,
        name = "RAG Ingest",
        description = "Chunks uploaded documents and writes RAG entries into the configured vector store",
        stability = OloStability.EXPERIMENTAL,
        category = "rag",
        emoji = "📚",
        tags = {"rag", "ingest", "vector", "documents", "plugin"},
        examples = {
            "Index finance-rag PDFs after upload",
            "Reprocess selected files into pgvector-store collection"
        },
        executionModel = OloExecutionModel.ACTIVITY,
        arguments = {
            @OloProperty(
                    name = "capabilitySource",
                    label = "Capability source",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Upload folder key / RAG collection id",
                    placeholder = "finance-rag",
                    group = "Documents",
                    order = 0),
            @OloProperty(
                    name = "fileNamesCsv",
                    label = "File names",
                    type = OloPropertyType.TEXTAREA,
                    description = "Comma-separated file names to index (empty = all files in source)",
                    placeholder = "policy.pdf,faq.md",
                    group = "Documents",
                    order = 1),
            @OloProperty(
                    name = "chunkSize",
                    label = "Chunk size",
                    type = OloPropertyType.NUMBER,
                    defaultValue = "512",
                    description = "Maximum characters per indexed chunk",
                    group = "Indexing",
                    order = 2),
            @OloProperty(
                    name = "extensionRef",
                    label = "Vector store extension",
                    type = OloPropertyType.STRING,
                    defaultValue = "pgvector-store",
                    description = "Workflow extensions[] id for VECTOR_STORE configuration",
                    group = "Vector store",
                    order = 3),
            @OloProperty(
                    name = "embeddingProviderRef",
                    label = "Embedding provider",
                    type = OloPropertyType.MODEL_SELECTOR,
                    description = "Embedding model provider ref from workflow modelProviders",
                    group = "Vector store",
                    order = 4)
        })
@ToolId(CoreToolIds.RAG_INGEST)
@ImplementationId(CoreToolIds.RAG_INGEST)
public final class RagIngestTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toolId() {
        return CoreToolIds.RAG_INGEST;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            Map<String, Object> arguments = new LinkedHashMap<>(
                    request.arguments() == null ? Map.of() : request.arguments());
            mergeMessagePayload(arguments, context);

            String capabilitySource = ToolArgs.string(arguments, "capabilitySource", "");
            if (capabilitySource.isBlank()) {
                capabilitySource = ToolArgs.string(arguments, "ragTag", "");
            }
            if (capabilitySource.isBlank()) {
                return ToolResult.failure("capabilitySource is required for RAG ingest", null);
            }

            Map<String, Object> toolConfiguration =
                    request.configuration() == null ? Map.of() : request.configuration();
            List<String> fileNames = RagVectorStoreSupport.readFileNames(arguments);
            int chunkSize = RagVectorStoreSupport.readChunkSize(toolConfiguration, arguments);
            Map<String, Object> extensionConfig = RagVectorStoreSupport.extensionConfigFrom(toolConfiguration);
            if (extensionConfig.isEmpty()) {
                extensionConfig = defaultExtensionConfig(arguments, toolConfiguration);
            }

            Path uploadBase = Paths.get(System.getProperty(
                    "olo.resource.upload.base-dir",
                    System.getenv().getOrDefault("OLO_RESOURCE_UPLOAD_DIR", System.getProperty("java.io.tmpdir") + "/olo-resource-uploads")));
            Path vectorIndexDir = Paths.get(System.getenv().getOrDefault(
                    "OLO_VECTOR_INDEX_DIR",
                    System.getProperty("java.io.tmpdir") + "/olo-vector-index"));

            RagVectorStoreSupport.IngestResult result = RagVectorStoreSupport.ingestDocuments(
                    uploadBase,
                    vectorIndexDir,
                    capabilitySource,
                    fileNames,
                    chunkSize,
                    extensionConfig);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("capabilitySource", result.capabilitySource());
            output.put("filesProcessed", result.filesProcessed());
            output.put("chunksIndexed", result.chunksIndexed());
            output.put("indexPath", result.indexPath());
            output.put("fileResults", result.fileResults());
            output.put("extensionRef", RagVectorStoreSupport.readExtensionRef(toolConfiguration));
            output.put("status", "INDEXED");

            String message = "Indexed "
                    + result.chunksIndexed()
                    + " chunks from "
                    + result.filesProcessed()
                    + " file(s) for "
                    + result.capabilitySource();
            return ToolResult.success(message, output);
        } catch (Exception e) {
            return ToolResult.failure("RAG ingest failed: " + failureMessage(e), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeMessagePayload(Map<String, Object> arguments, ExecutionContext context) {
        Object rawMessage = firstPresent(arguments, "message", "userQuery", "query", "text");
        if (rawMessage instanceof Map<?, ?> payload) {
            mergePayloadMap(arguments, (Map<String, Object>) payload);
            return;
        }

        String message = rawMessage == null ? "" : String.valueOf(rawMessage);
        if (message.isBlank()) {
            mergeRagTagFallback(arguments, context);
            return;
        }
        if (!message.trim().startsWith("{")) {
            mergeRagTagFallback(arguments, context);
            return;
        }
        try {
            Map<String, Object> payload = MAPPER.readValue(message.trim(), new TypeReference<>() {});
            mergePayloadMap(arguments, payload);
        } catch (Exception ignored) {
            // keep explicit arguments
        }
    }

    private static Object firstPresent(Map<String, Object> arguments, String... keys) {
        for (String key : keys) {
            Object value = arguments.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static void mergePayloadMap(Map<String, Object> arguments, Map<String, Object> payload) {
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            arguments.putIfAbsent(entry.getKey(), entry.getValue());
        }
        Object files = payload.get("fileNames");
        if (files instanceof List<?> list && !list.isEmpty()) {
            arguments.put("fileNames", list);
        }
    }

    private static String failureMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private static void mergeRagTagFallback(Map<String, Object> arguments, ExecutionContext context) {
        Object ragTag = arguments.get("ragTag");
        if (ragTag == null || String.valueOf(ragTag).isBlank()) {
            ragTag = arguments.get("capabilitySource");
        }
        if (ragTag == null || String.valueOf(ragTag).isBlank()) {
            ragTag = context.getVariable("ragTag");
        }
        if (ragTag == null || String.valueOf(ragTag).isBlank()) {
            ragTag = context.getVariable("capabilitySource");
        }
        if (ragTag != null && !String.valueOf(ragTag).isBlank()) {
            arguments.putIfAbsent("capabilitySource", String.valueOf(ragTag));
            arguments.putIfAbsent("ragTag", String.valueOf(ragTag));
        }
    }

    private static Map<String, Object> defaultExtensionConfig(
            Map<String, Object> arguments,
            Map<String, Object> toolConfiguration) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("driver", "file-json");
        config.put(
                "connectionRef",
                "${env:OLO_VECTOR_INDEX_DIR}");
        config.put("table", ToolArgs.string(toolConfiguration, "vectorTable", "documents"));
        String extensionRef = ToolArgs.string(arguments, "extensionRef", "");
        if (extensionRef.isBlank()) {
            extensionRef = RagVectorStoreSupport.readExtensionRef(toolConfiguration);
        }
        config.put("extensionRef", extensionRef);
        return config;
    }
}
