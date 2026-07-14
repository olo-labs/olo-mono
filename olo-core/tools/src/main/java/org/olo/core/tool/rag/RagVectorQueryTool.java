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
import org.olo.core.tool.ConversationPluginSupport;
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
 * Retrieves relevant document chunks from the configured vector store and exposes them as {@code ragContext}.
 */
@OloTool(
        id = CoreToolIds.RAG_QUERY,
        name = "RAG Query",
        description = "Searches indexed documents for the user question and injects retrieved context into the prompt",
        stability = OloStability.EXPERIMENTAL,
        category = "rag",
        emoji = "🔎",
        tags = {"rag", "query", "vector", "retrieval", "plugin"},
        examples = {
            "Retrieve policy excerpts for finance-rag before answering",
            "Ground the agent response with indexed FAQ chunks"
        },
        executionModel = OloExecutionModel.ACTIVITY,
        arguments = {
            @OloProperty(
                    name = "capabilitySource",
                    label = "Capability source",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Indexed knowledge source / collection id",
                    placeholder = "finance-rag",
                    group = "Retrieval",
                    order = 0),
            @OloProperty(
                    name = "query",
                    label = "Query",
                    type = OloPropertyType.TEXTAREA,
                    description = "User question (defaults to workflow message)",
                    group = "Retrieval",
                    order = 1),
            @OloProperty(
                    name = "topK",
                    label = "Top K",
                    type = OloPropertyType.NUMBER,
                    defaultValue = "5",
                    group = "Retrieval",
                    order = 2),
            @OloProperty(
                    name = "scoreThreshold",
                    label = "Score threshold",
                    type = OloPropertyType.NUMBER,
                    defaultValue = "0.25",
                    group = "Retrieval",
                    order = 3),
            @OloProperty(
                    name = "extensionRef",
                    label = "Vector store",
                    type = OloPropertyType.STRING,
                    defaultValue = "pgvector-store",
                    group = "Vector store",
                    order = 4)
        })
@ToolId(CoreToolIds.RAG_QUERY)
@ImplementationId(CoreToolIds.RAG_QUERY)
public final class RagVectorQueryTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toolId() {
        return CoreToolIds.RAG_QUERY;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            Map<String, Object> arguments = new LinkedHashMap<>(
                    request.arguments() == null ? Map.of() : request.arguments());
            mergePayload(arguments, context);

            String capabilitySource = readCapabilitySource(arguments, context);
            if (capabilitySource.isBlank()) {
                return ToolResult.failure("capabilitySource is required for RAG query", null);
            }

            String query = ToolArgs.string(arguments, "query", "");
            if (query.isBlank()) {
                query = ToolArgs.string(arguments, ConversationPluginSupport.MESSAGE_VARIABLE, "");
            }
            if (query.isBlank()) {
                Object messageVariable = context.getVariable(ConversationPluginSupport.MESSAGE_VARIABLE);
                query = messageVariable == null ? "" : String.valueOf(messageVariable);
            }
            if (query.isBlank()) {
                return ToolResult.failure("query is required for RAG query", null);
            }

            Map<String, Object> toolConfiguration =
                    request.configuration() == null ? Map.of() : request.configuration();
            int topK = RagVectorStoreSupport.readTopK(toolConfiguration, arguments);
            double scoreThreshold = RagVectorStoreSupport.readScoreThreshold(toolConfiguration, arguments);
            Map<String, Object> extensionConfig = RagVectorStoreSupport.extensionConfigFrom(toolConfiguration);
            if (extensionConfig.isEmpty()) {
                extensionConfig = defaultExtensionConfig(arguments, toolConfiguration);
            }

            Path vectorIndexDir = Paths.get(System.getenv().getOrDefault(
                    "OLO_VECTOR_INDEX_DIR",
                    System.getProperty("java.io.tmpdir") + "/olo-vector-index"));

            RagVectorStoreSupport.SearchResult result = RagVectorStoreSupport.search(
                    vectorIndexDir,
                    capabilitySource,
                    query,
                    topK,
                    scoreThreshold,
                    extensionConfig);

            String ragContext = result.ragContext() == null ? "" : result.ragContext().trim();
            context.setVariable(ConversationPluginSupport.RAG_CONTEXT_VARIABLE, ragContext);
            context.setVariable("capabilitySource", capabilitySource);

            String enrichedMessage = enrichMessage(query, ragContext);
            context.setVariable(ConversationPluginSupport.MESSAGE_VARIABLE, enrichedMessage);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("capabilitySource", capabilitySource);
            output.put("query", query);
            output.put("hits", result.hits());
            output.put(ConversationPluginSupport.RAG_CONTEXT_VARIABLE, ragContext);
            output.put("matches", result.matches());
            output.put("message", enrichedMessage);

            String message = result.hits() > 0
                    ? "Retrieved " + result.hits() + " chunk(s) for " + capabilitySource
                    : "No indexed chunks matched for " + capabilitySource;
            return ToolResult.success(message, output);
        } catch (Exception e) {
            return ToolResult.failure("RAG query failed: " + e.getMessage(), e);
        }
    }

    private static String enrichMessage(String query, String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return query == null ? "" : query;
        }
        return "Retrieved knowledge context:\n"
                + ragContext
                + "\n\nUser question:\n"
                + (query == null ? "" : query);
    }

    @SuppressWarnings("unchecked")
    private static void mergePayload(Map<String, Object> arguments, ExecutionContext context) {
        String message = ToolArgs.string(arguments, "message", "");
        if (message.isBlank()) {
            message = ToolArgs.string(arguments, "userQuery", "");
        }
        if (message.isBlank() || !message.trim().startsWith("{")) {
            mergeCapabilitySourceFallback(arguments, context);
            return;
        }
        try {
            Map<String, Object> payload = MAPPER.readValue(message.trim(), new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                arguments.putIfAbsent(entry.getKey(), entry.getValue());
            }
            Object userMessage = payload.get("message");
            if (userMessage != null && !String.valueOf(userMessage).isBlank()) {
                arguments.put("query", String.valueOf(userMessage));
            }
            mergeCapabilitySourceFallback(arguments, context);
        } catch (Exception ignored) {
            // keep explicit arguments
            mergeCapabilitySourceFallback(arguments, context);
        }
    }

    private static String readCapabilitySource(Map<String, Object> arguments, ExecutionContext context) {
        mergeCapabilitySourceFallback(arguments, context);
        String capabilitySource = ToolArgs.string(arguments, "capabilitySource", "");
        if (capabilitySource.isBlank()) {
            capabilitySource = ToolArgs.string(arguments, "ragTag", "");
        }
        return capabilitySource;
    }

    private static void mergeCapabilitySourceFallback(Map<String, Object> arguments, ExecutionContext context) {
        Object ragTag = firstNonBlank(
                arguments.get("ragTag"),
                arguments.get("capabilitySource"),
                arguments.get("knowledgeSource"),
                arguments.get("ragSource"),
                context.getVariable("ragTag"),
                context.getVariable("capabilitySource"));
        if (ragTag != null) {
            String value = String.valueOf(ragTag).trim();
            arguments.putIfAbsent("capabilitySource", value);
            arguments.putIfAbsent("ragTag", value);
        }
    }

    private static Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
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
}
