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
import java.util.Map;

/**
 * Deletes a tokenized RAG knowledge entry from the configured vector store.
 */
@OloTool(
        id = CoreToolIds.RAG_DELETE,
        name = "RAG Delete",
        description = "Deletes RAG entries from the configured vector store",
        stability = OloStability.EXPERIMENTAL,
        category = "rag",
        emoji = "X",
        tags = {"rag", "delete", "vector", "documents", "plugin"},
        examples = {
            "Delete finance-rag from the vector store",
            "Remove a tokenized knowledge source from Qdrant"
        },
        executionModel = OloExecutionModel.ACTIVITY,
        arguments = {
            @OloProperty(
                    name = "knowledgeName",
                    label = "Knowledge source",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Final tokenized knowledge source name / RAG tag to delete",
                    placeholder = "finance-rag",
                    group = "Knowledge",
                    order = 0),
            @OloProperty(
                    name = "sourceCollection",
                    label = "Source collection",
                    type = OloPropertyType.STRING,
                    description = "Original uploaded source collection, when different from the final knowledge name",
                    placeholder = "finance-uploads",
                    group = "Knowledge",
                    order = 1),
            @OloProperty(
                    name = "extensionRef",
                    label = "Vector store extension",
                    type = OloPropertyType.STRING,
                    defaultValue = "pgvector-store",
                    description = "Workflow extensions[] id for VECTOR_STORE configuration",
                    group = "Vector store",
                    order = 2)
        })
@ToolId(CoreToolIds.RAG_DELETE)
@ImplementationId(CoreToolIds.RAG_DELETE)
public final class RagDeleteTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toolId() {
        return CoreToolIds.RAG_DELETE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            Map<String, Object> arguments = new LinkedHashMap<>(
                    request.arguments() == null ? Map.of() : request.arguments());
            mergeMessagePayload(arguments, context);

            String knowledgeName = ToolArgs.string(arguments, "knowledgeName", "");
            if (knowledgeName.isBlank()) {
                knowledgeName = ToolArgs.string(arguments, "ragTag", "");
            }
            if (knowledgeName.isBlank()) {
                knowledgeName = ToolArgs.string(arguments, "capabilitySource", "");
            }
            if (knowledgeName.isBlank()) {
                return ToolResult.failure("knowledgeName is required for RAG delete", null);
            }

            String sourceCollection = ToolArgs.string(arguments, "sourceCollection", "");
            Map<String, Object> toolConfiguration =
                    request.configuration() == null ? Map.of() : request.configuration();
            Map<String, Object> extensionConfig = RagVectorStoreSupport.extensionConfigFrom(toolConfiguration);
            if (extensionConfig.isEmpty()) {
                extensionConfig = defaultExtensionConfig(arguments, toolConfiguration);
            }

            Path vectorIndexDir = Paths.get(System.getenv().getOrDefault(
                    "OLO_VECTOR_INDEX_DIR",
                    System.getProperty("java.io.tmpdir") + "/olo-vector-index"));

            RagVectorStoreSupport.DeleteResult result = RagVectorStoreSupport.deleteKnowledge(
                    vectorIndexDir,
                    knowledgeName,
                    sourceCollection,
                    extensionConfig);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("knowledgeName", result.knowledgeName());
            output.put("deletedSources", result.deletedSources());
            output.put("chunksDeleted", result.chunksDeleted());
            output.put("filesAffected", result.filesAffected());
            output.put("indexPath", result.indexPath());
            output.put("extensionRef", RagVectorStoreSupport.readExtensionRef(toolConfiguration));
            output.put("status", "DELETED");

            String message = "Deleted RAG knowledge source " + result.knowledgeName()
                    + " from vector store";
            return ToolResult.success(message, output);
        } catch (Exception e) {
            return ToolResult.failure("RAG delete failed: " + failureMessage(e), e);
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
        if (message.isBlank() || !message.trim().startsWith("{")) {
            mergeContextFallback(arguments, context);
            return;
        }
        try {
            Map<String, Object> payload = MAPPER.readValue(message.trim(), new TypeReference<>() {});
            mergePayloadMap(arguments, payload);
        } catch (Exception ignored) {
            mergeContextFallback(arguments, context);
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
    }

    private static void mergeContextFallback(Map<String, Object> arguments, ExecutionContext context) {
        Object ragTag = arguments.get("ragTag");
        if (ragTag == null || String.valueOf(ragTag).isBlank()) {
            ragTag = context.getVariable("ragTag");
        }
        if (ragTag == null || String.valueOf(ragTag).isBlank()) {
            ragTag = context.getVariable("knowledgeName");
        }
        if (ragTag != null && !String.valueOf(ragTag).isBlank()) {
            arguments.putIfAbsent("knowledgeName", String.valueOf(ragTag));
            arguments.putIfAbsent("ragTag", String.valueOf(ragTag));
        }
    }

    private static Map<String, Object> defaultExtensionConfig(
            Map<String, Object> arguments,
            Map<String, Object> toolConfiguration) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("driver", "file-json");
        config.put("connectionRef", "${env:OLO_VECTOR_INDEX_DIR}");
        config.put("table", ToolArgs.string(toolConfiguration, "vectorTable", "documents"));
        String extensionRef = ToolArgs.string(arguments, "extensionRef", "");
        if (extensionRef.isBlank()) {
            extensionRef = RagVectorStoreSupport.readExtensionRef(toolConfiguration);
        }
        config.put("extensionRef", extensionRef);
        return config;
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
}
