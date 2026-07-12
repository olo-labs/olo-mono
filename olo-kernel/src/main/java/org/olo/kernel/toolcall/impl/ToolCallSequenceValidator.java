/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.kernel.toolcall.model.ParsedAgentCall;
import org.olo.kernel.toolcall.model.ParsedToolCall;
import org.olo.kernel.toolcall.model.ToolCallValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ToolCallSequenceValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolCallSequenceValidator() {
    }

    public static ToolCallValidationResult validate(
            String rawJson, String allowedToolsJson, String allowedAgentsJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ToolCallValidationResult.invalid("tool call sequence JSON is blank");
        }
        try {
            String normalized = stripMarkdownFences(rawJson.trim());
            JsonNode root = MAPPER.readTree(normalized);
            if (!root.isObject()) {
                return ToolCallValidationResult.invalid("tool call sequence JSON must be a single object");
            }
            JsonNode toolCalls = root.get("toolCalls");
            if (toolCalls == null || !toolCalls.isArray()) {
                return ToolCallValidationResult.invalid("tool call sequence JSON requires a toolCalls array");
            }
            JsonNode agentCalls = root.has("agentCalls") ? root.get("agentCalls") : null;
            if (agentCalls != null && !agentCalls.isArray()) {
                return ToolCallValidationResult.invalid(
                        "tool call sequence JSON requires agentCalls to be an array when present");
            }
            if (!root.has("directResponse")) {
                return ToolCallValidationResult.invalid(
                        "tool call sequence JSON requires directResponse (string or null)");
            }
            JsonNode directResponseNode = root.get("directResponse");
            String directResponse = directResponseNode == null || directResponseNode.isNull()
                    ? null
                    : directResponseNode.asText();
            Set<String> allowedToolIds = parseAllowedToolIds(allowedToolsJson);
            Set<String> allowedAgentIds = parseAllowedAgentIds(allowedAgentsJson);
            List<ParsedToolCall> parsedCalls = new ArrayList<>();
            List<ParsedAgentCall> parsedAgentCalls = new ArrayList<>();
            for (JsonNode call : toolCalls) {
                if (!call.isObject()) {
                    return ToolCallValidationResult.invalid("each toolCalls entry must be an object");
                }
                String toolId = text(call, "toolId");
                if (toolId == null || toolId.isBlank()) {
                    return ToolCallValidationResult.invalid("each toolCalls entry requires toolId");
                }
                if (!allowedToolIds.isEmpty() && !allowedToolIds.contains(toolId)) {
                    return ToolCallValidationResult.invalid(
                            "toolId is not in availableToolsJson allow-list: " + toolId);
                }
                Map<String, Object> arguments = Map.of();
                JsonNode argumentsNode = call.get("arguments");
                if (argumentsNode != null && !argumentsNode.isNull()) {
                    if (!argumentsNode.isObject()) {
                        return ToolCallValidationResult.invalid(
                                "toolCalls.arguments must be an object when present");
                    }
                    arguments = MAPPER.convertValue(argumentsNode, Map.class);
                }
                parsedCalls.add(new ParsedToolCall(toolId, arguments));
            }
            if (agentCalls != null) {
                for (JsonNode call : agentCalls) {
                    if (!call.isObject()) {
                        return ToolCallValidationResult.invalid("each agentCalls entry must be an object");
                    }
                    String agentId = text(call, "agentId");
                    if (agentId == null || agentId.isBlank()) {
                        return ToolCallValidationResult.invalid("each agentCalls entry requires agentId");
                    }
                    if (!allowedAgentIds.isEmpty() && !allowedAgentIds.contains(agentId)) {
                        return ToolCallValidationResult.invalid(
                                "agentId is not in availableAgentsJson allow-list: " + agentId);
                    }
                    String message = text(call, "message");
                    parsedAgentCalls.add(new ParsedAgentCall(agentId, message));
                }
            }
            if (!parsedCalls.isEmpty() || !parsedAgentCalls.isEmpty()) {
                if (directResponse != null && !directResponse.isBlank()) {
                    return ToolCallValidationResult.invalid(
                            "directResponse must be null when toolCalls or agentCalls is non-empty");
                }
                return ToolCallValidationResult.calls(normalized, parsedCalls, parsedAgentCalls);
            }
            if (directResponse == null || directResponse.isBlank()) {
                return ToolCallValidationResult.invalid(
                        "directResponse is required when toolCalls and agentCalls are empty");
            }
            return ToolCallValidationResult.directResponse(normalized, directResponse);
        } catch (Exception e) {
            return ToolCallValidationResult.invalid(
                    "tool call sequence JSON is not valid JSON: " + e.getMessage());
        }
    }

    static Set<String> parseAllowedToolIds(String allowedToolsJson) {
        Set<String> allowed = new LinkedHashSet<>();
        if (allowedToolsJson == null || allowedToolsJson.isBlank()) {
            return allowed;
        }
        try {
            JsonNode root = MAPPER.readTree(allowedToolsJson.trim());
            if (!root.isArray()) {
                return allowed;
            }
            for (JsonNode entry : root) {
                if (entry.isObject()) {
                    String toolId = text(entry, "toolId");
                    if (toolId != null && !toolId.isBlank()) {
                        allowed.add(toolId);
                    }
                } else if (entry.isTextual()) {
                    allowed.add(entry.asText());
                }
            }
        } catch (Exception ignored) {
            return allowed;
        }
        return allowed;
    }

    static Set<String> parseAllowedAgentIds(String allowedAgentsJson) {
        Set<String> allowed = new LinkedHashSet<>();
        if (allowedAgentsJson == null || allowedAgentsJson.isBlank()) {
            return allowed;
        }
        try {
            JsonNode root = MAPPER.readTree(allowedAgentsJson.trim());
            if (!root.isArray()) {
                return allowed;
            }
            for (JsonNode entry : root) {
                if (entry.isObject()) {
                    String agentId = text(entry, "agentId");
                    if (agentId != null && !agentId.isBlank()) {
                        allowed.add(agentId);
                    }
                } else if (entry.isTextual()) {
                    allowed.add(entry.asText());
                }
            }
        } catch (Exception ignored) {
            return allowed;
        }
        return allowed;
    }

    private static String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            int closingFence = text.lastIndexOf("```");
            if (firstLineBreak >= 0 && closingFence > firstLineBreak) {
                return text.substring(firstLineBreak + 1, closingFence).trim();
            }
        }
        return text;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
