/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.dynamicgraph.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.node.NodeType;
import org.olo.kernel.dynamicgraph.model.DynamicSubgraphValidationResult;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Validates planner-produced subgraph JSON before merge.
 */
final class DynamicSubgraphJsonValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DynamicSubgraphJsonValidator() {
    }

    static DynamicSubgraphValidationResult validate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return DynamicSubgraphValidationResult.invalid("generated graph JSON is blank");
        }
        try {
            String normalized = stripMarkdownFences(rawJson.trim());
            JsonNode root = MAPPER.readTree(normalized);
            if (!root.isObject()) {
                return DynamicSubgraphValidationResult.invalid("generated graph JSON must be a single object");
            }
            if (!root.hasNonNull("id") || root.get("id").asText().isBlank()) {
                return DynamicSubgraphValidationResult.invalid("generated graph JSON requires non-blank id");
            }
            JsonNode nodes = root.get("nodes");
            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                return DynamicSubgraphValidationResult.invalid("generated graph JSON requires a non-empty nodes array");
            }
            JsonNode edges = root.get("edges");
            if (edges == null || !edges.isArray()) {
                return DynamicSubgraphValidationResult.invalid("generated graph JSON requires an edges array");
            }
            Set<String> nodeIds = new LinkedHashSet<>();
            boolean hasStart = false;
            boolean hasEnd = false;
            for (JsonNode node : nodes) {
                if (!node.isObject()) {
                    return DynamicSubgraphValidationResult.invalid("each node entry must be an object");
                }
                String id = text(node, "id");
                String type = text(node, "type");
                if (id == null || id.isBlank() || type == null || type.isBlank()) {
                    return DynamicSubgraphValidationResult.invalid("each node requires id and type");
                }
                if (!nodeIds.add(id)) {
                    return DynamicSubgraphValidationResult.invalid("duplicate node id in generated graph JSON: " + id);
                }
                if (NodeType.START.name().equals(type)) {
                    hasStart = true;
                }
                if (NodeType.END.name().equals(type)) {
                    hasEnd = true;
                }
                if (NodeType.TOOL.name().equals(type)) {
                    JsonNode configuration = node.get("configuration");
                    String toolId = configuration == null ? null : text(configuration, "toolId");
                    if (toolId == null || toolId.isBlank()) {
                        return DynamicSubgraphValidationResult.invalid(
                                "TOOL node '" + id + "' requires configuration.toolId");
                    }
                }
                if (!isAllowedNodeType(type)) {
                    return DynamicSubgraphValidationResult.invalid(
                            "invalid node type '"
                                    + type
                                    + "' for node '"
                                    + id
                                    + "'. Use a single NodeType value such as START, END, AGENT, or TOOL");
                }
            }
            if (!hasStart || !hasEnd) {
                return DynamicSubgraphValidationResult.invalid("generated graph JSON must include START and END nodes");
            }
            for (JsonNode edge : edges) {
                if (!edge.isObject()) {
                    return DynamicSubgraphValidationResult.invalid("each edge entry must be an object");
                }
                String sourceNodeId = text(edge, "sourceNodeId");
                String targetNodeId = text(edge, "targetNodeId");
                if (sourceNodeId == null
                        || sourceNodeId.isBlank()
                        || targetNodeId == null
                        || targetNodeId.isBlank()) {
                    return DynamicSubgraphValidationResult.invalid("each edge requires sourceNodeId and targetNodeId");
                }
                if (!nodeIds.contains(sourceNodeId) || !nodeIds.contains(targetNodeId)) {
                    return DynamicSubgraphValidationResult.invalid("edge references unknown node id");
                }
            }
            return DynamicSubgraphValidationResult.valid(normalized);
        } catch (Exception e) {
            return DynamicSubgraphValidationResult.invalid(
                    "generated graph JSON is not valid JSON: " + e.getMessage());
        }
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

    private static boolean isAllowedNodeType(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        try {
            NodeType.valueOf(type.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
