/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.output;

import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves which {@link ExecutionOutputs} slot is returned to the caller.
 */
public final class WorkflowReturnOutput {

    /** Workflow {@link WorkflowDefinition#getMetadata()} key naming the return output slot. */
    public static final String WORKFLOW_METADATA_KEY = "returnOutputKey";

    /** Node {@code configuration} key overriding the output map slot (defaults to node id). */
    public static final String NODE_OUTPUT_KEY = "outputKey";

    private WorkflowReturnOutput() {
    }

    public static String readReturnOutputKey(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        return readWorkflowMetadata(graph.getMetadata());
    }

    public static String outputKeyForNode(NodeDefinition node) {
        Objects.requireNonNull(node, "node");
        Map<String, Object> configuration = node.getConfiguration();
        if (configuration != null && !configuration.isEmpty()) {
            Object alias = configuration.get(NODE_OUTPUT_KEY);
            if (alias instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        String nodeId = node.getId();
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("node has no id for execution output key");
        }
        return nodeId.trim();
    }

    public static boolean shouldMirrorToReturnVariable(
            WorkflowDefinition graph, NodeDefinition node, String returnVariableName) {
        if (returnVariableName == null || returnVariableName.isBlank()) {
            return false;
        }
        if (DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)) {
            return false;
        }
        String configuredReturnKey = readReturnOutputKey(graph);
        if (configuredReturnKey == null) {
            return true;
        }
        return configuredReturnKey.equals(outputKeyForNode(node));
    }

    private static String readWorkflowMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object value = metadata.get(WORKFLOW_METADATA_KEY);
        if (value instanceof String name && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }
}
