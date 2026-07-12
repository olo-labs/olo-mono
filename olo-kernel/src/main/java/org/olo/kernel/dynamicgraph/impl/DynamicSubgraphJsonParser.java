/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.dynamicgraph.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses validated subgraph JSON into workflow definition node and edge models.
 */
final class DynamicSubgraphJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DynamicSubgraphJsonParser() {
    }

    static List<NodeDefinition> parseNodes(JsonNode nodes) {
        List<NodeDefinition> parsed = new ArrayList<>();
        for (JsonNode node : nodes) {
            NodeDefinitionBuilder builder = NodeDefinition.builder()
                    .id(text(node, "id"))
                    .type(text(node, "type"));
            String label = text(node, "label");
            if (label != null && !label.isBlank()) {
                builder.label(label);
            }
            JsonNode configuration = node.get("configuration");
            if (configuration != null && configuration.isObject()) {
                builder.configuration(MAPPER.convertValue(configuration, Map.class));
            }
            parsed.add(builder.build());
        }
        return parsed;
    }

    static List<EdgeDefinition> parseEdges(JsonNode edges) {
        List<EdgeDefinition> parsed = new ArrayList<>();
        for (JsonNode edge : edges) {
            parsed.add(EdgeDefinition.builder()
                    .sourceNodeId(text(edge, "sourceNodeId"))
                    .sourcePortId(Optional.ofNullable(text(edge, "sourcePortId")).orElse("out"))
                    .targetNodeId(text(edge, "targetNodeId"))
                    .targetPortId(Optional.ofNullable(text(edge, "targetPortId")).orElse("in"))
                    .build());
        }
        return parsed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
