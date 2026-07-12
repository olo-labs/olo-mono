/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.olo.core.tool.observability.ObservabilitySupport;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.ToolRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** File-backed demo conversation history for load/store workflow plugins. */
public final class ConversationPluginSupport {

    public static final String CONVERSATION_SUMMARY_VARIABLE = "conversationSummary";
    public static final String CONVERSATION_HISTORY_VARIABLE = "conversationHistory";
    public static final String MESSAGE_VARIABLE = "message";
    public static final String RETURN_VALUE_VARIABLE = "ReturnValue";

    public static final String DEFAULT_CONVERSATION_FOLDER = "demo-data/conversations";
    public static final String DEFAULT_SESSION_ID = "demo-session";

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<Map<String, Object>>> TURN_LIST_TYPE = new TypeReference<>() {};

    private ConversationPluginSupport() {
    }

    public static String resolveSessionId(ToolRequest request, ExecutionContext context) {
        String sessionId = ToolArgs.string(request.arguments(), "sessionId", "");
        if (!sessionId.isBlank()) {
            return sessionId;
        }
        Object fromContext = context.getVariable("sessionId");
        if (fromContext != null && !String.valueOf(fromContext).isBlank()) {
            return String.valueOf(fromContext);
        }
        String correlationId = context.getCorrelationId().orElse("");
        if (!correlationId.isBlank()) {
            return correlationId;
        }
        return context.getRunId() == null || context.getRunId().isBlank()
                ? DEFAULT_SESSION_ID
                : context.getRunId();
    }

    public static Path resolveConversationFile(String sessionId) {
        Path toolsDirectory = ObservabilitySupport.discoverToolsDirectory();
        Path folder = toolsDirectory == null
                ? Path.of(DEFAULT_CONVERSATION_FOLDER)
                : toolsDirectory.resolve(DEFAULT_CONVERSATION_FOLDER);
        String safeSession = sessionId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return folder.resolve(safeSession + ".json");
    }

    public static List<Map<String, Object>> readHistory(String sessionId) throws IOException {
        Path file = resolveConversationFile(sessionId);
        if (!Files.exists(file)) {
            return List.of();
        }
        String content = Files.readString(file);
        if (content.isBlank()) {
            return List.of();
        }
        return JSON.readValue(content, TURN_LIST_TYPE);
    }

    public static void writeHistory(String sessionId, List<Map<String, Object>> history) throws IOException {
        Path file = resolveConversationFile(sessionId);
        Files.createDirectories(file.getParent());
        Files.writeString(file, JSON.writeValueAsString(history));
    }

    public static String buildSummary(List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return "No prior conversation.";
        }
        int start = Math.max(0, history.size() - 4);
        StringBuilder summary = new StringBuilder();
        for (int index = start; index < history.size(); index++) {
            Map<String, Object> turn = history.get(index);
            String role = String.valueOf(turn.getOrDefault("role", "user"));
            String text = String.valueOf(turn.getOrDefault("content", ""));
            if (text.isBlank()) {
                continue;
            }
            if (!summary.isEmpty()) {
                summary.append(' ');
            }
            summary.append(role).append(": ").append(truncate(text, 180));
        }
        return summary.isEmpty() ? "No prior conversation." : summary.toString();
    }

    public static String attachSummaryToMessage(String summary, String message) {
        String current = message == null ? "" : message;
        if (summary == null || summary.isBlank() || "No prior conversation.".equals(summary)) {
            return current;
        }
        return "Previous conversation summary:\n" + summary + "\n\nCurrent request:\n" + current;
    }

    public static Map<String, Object> loadConversationContext(
            ToolRequest request, ExecutionContext context, String currentMessage) throws IOException {
        String sessionId = resolveSessionId(request, context);
        List<Map<String, Object>> history = new ArrayList<>(readHistory(sessionId));
        String summary = buildSummary(history);
        String enrichedMessage = attachSummaryToMessage(summary, currentMessage);

        context.setVariable("sessionId", sessionId);
        context.setVariable(CONVERSATION_SUMMARY_VARIABLE, summary);
        context.setVariable(CONVERSATION_HISTORY_VARIABLE, JSON.writeValueAsString(history));
        context.setVariable(MESSAGE_VARIABLE, enrichedMessage);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sessionId", sessionId);
        output.put(CONVERSATION_SUMMARY_VARIABLE, summary);
        output.put("turnCount", history.size());
        output.put("storagePath", resolveConversationFile(sessionId).toAbsolutePath().normalize().toString());
        output.put("messageAttached", !summary.equals("No prior conversation."));
        return output;
    }

    public static Map<String, Object> storeConversationTurn(
            ToolRequest request, ExecutionContext context) throws IOException {
        String sessionId = resolveSessionId(request, context);
        List<Map<String, Object>> history = new ArrayList<>(readHistory(sessionId));
        String userMessage = ToolArgs.string(request.arguments(), MESSAGE_VARIABLE, "");
        if (userMessage.isBlank()) {
            Object messageVariable = context.getVariable(MESSAGE_VARIABLE);
            userMessage = messageVariable == null ? "" : String.valueOf(messageVariable);
        }
        String assistantResponse = ToolArgs.string(request.arguments(), RETURN_VALUE_VARIABLE, "");
        if (assistantResponse.isBlank()) {
            Object returnValue = context.getVariable(RETURN_VALUE_VARIABLE);
            assistantResponse = returnValue == null ? "" : String.valueOf(returnValue);
        }

        if (!userMessage.isBlank()) {
            history.add(turn("user", userMessage));
        }
        if (!assistantResponse.isBlank()) {
            history.add(turn("assistant", assistantResponse));
        }
        writeHistory(sessionId, history);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sessionId", sessionId);
        output.put("storedTurns", history.size());
        output.put("storagePath", resolveConversationFile(sessionId).toAbsolutePath().normalize().toString());
        output.put("summary", buildSummary(history));
        return output;
    }

    private static Map<String, Object> turn(String role, String content) {
        Map<String, Object> turn = new LinkedHashMap<>();
        turn.put("role", role);
        turn.put("content", content);
        turn.put("timestamp", Instant.now().toString());
        return turn;
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
