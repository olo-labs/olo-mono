/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.dynamicgraph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;

/**
 * Parses dynamic subgraph injection audit documents ({@code mergedGraph} envelope) for Studio and bootstrap loading.
 */
public final class DynamicSubgraphInjectionSupport {

    public static final String FIELD_MERGED_GRAPH = "mergedGraph";
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_WORKFLOW_ID = "workflowId";
    public static final String FIELD_PLANNER_NODE_ID = "plannerNodeId";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String METADATA_INJECTED_SUBGRAPH = "injectedSubgraph";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonWorkflowSerializer WORKFLOW_JSON = new JsonWorkflowSerializer();

    private DynamicSubgraphInjectionSupport() {
    }

    public static boolean isInjectionDocument(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return false;
        }
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            return root.isObject() && root.hasNonNull(FIELD_MERGED_GRAPH);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static WorkflowDefinition loadBuilderWorkflow(String rawJson) throws IOException {
        JsonNode root = MAPPER.readTree(rawJson);
        JsonNode mergedGraph = root.get(FIELD_MERGED_GRAPH);
        if (mergedGraph == null || mergedGraph.isNull()) {
            throw new IOException("injection document missing mergedGraph");
        }
        WorkflowDefinition merged = WORKFLOW_JSON.deserialize(MAPPER.writeValueAsString(mergedGraph));
        String kind = text(root, FIELD_KIND);
        String workflowId = text(root, FIELD_WORKFLOW_ID);
        String plannerNodeId = text(root, FIELD_PLANNER_NODE_ID);
        String timestamp = text(root, FIELD_TIMESTAMP);
        String builderId = builderWorkflowId(kind, workflowId, plannerNodeId);
        return DynamicSubgraphStudioPreparer.prepareForBuilder(merged, builderId, kind, timestamp);
    }

    public static String builderWorkflowId(String kind, String workflowId, String plannerNodeId) {
        String base = sanitize(kind) + "-" + sanitize(workflowId);
        if (plannerNodeId != null
                && !plannerNodeId.isBlank()
                && !plannerNodeId.equals(workflowId)) {
            return base + "-" + sanitize(plannerNodeId);
        }
        return base;
    }

    public static boolean isToolSynthesis(NodeDefinition node) {
        return node != null
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("^-|-$", "");
    }
}
