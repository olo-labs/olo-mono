/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.snapshot.impl;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.exception.KernelException;

import java.io.IOException;
import java.util.Optional;

/**
 * Decides whether a traversal snapshot must embed the full workflow graph JSON in Temporal payloads.
 * When the graph still matches the worker registry entry, only {@code queue} and routing pipeline id are needed.
 */
public final class GraphSnapshotPolicy {

    private static final JsonWorkflowSerializer GRAPH_SERIALIZER = new JsonWorkflowSerializer();

    private GraphSnapshotPolicy() {
    }

    public static String maybeEmbedGraphJson(String queue, WorkflowInput input, WorkflowDefinition graph) {
        Optional<WorkflowDefinition> registered = optionalRegistryGraph(queue, workflowIdFromInput(input));
        if (registered.isPresent() && registered.get().equals(graph)) {
            return null;
        }
        return serializeGraph(graph);
    }

    public static WorkflowDefinition resolveGraph(String queue, WorkflowInput input, String graphJson) {
        if (graphJson != null && !graphJson.isBlank()) {
            return readGraph(graphJson);
        }
        WorkflowDefinitionRegistry registry = KernelRuntimeHolder.registry();
        return registry.resolve(queue, workflowIdFromInput(input))
                .orElseThrow(() -> new KernelException(
                        "graphJson is required when no workflow definition is registered for queue: " + queue
                                + (workflowIdFromInput(input) != null
                                        ? " (workflowId=" + workflowIdFromInput(input) + ")"
                                        : "")));
    }

    private static Optional<WorkflowDefinition> optionalRegistryGraph(String queue, String workflowId) {
        try {
            return KernelRuntimeHolder.registry().resolve(queue, workflowId);
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    private static String workflowIdFromInput(WorkflowInput input) {
        if (input == null || input.getRouting() == null) {
            return null;
        }
        String pipeline = input.getRouting().getPipeline();
        return pipeline != null && !pipeline.isBlank() ? pipeline.trim() : null;
    }

    private static String serializeGraph(WorkflowDefinition graph) {
        try {
            return GRAPH_SERIALIZER.serialize(graph);
        } catch (IOException e) {
            throw new KernelException("failed to serialize workflow graph for traversal snapshot", e);
        }
    }

    private static WorkflowDefinition readGraph(String graphJson) {
        try {
            return GRAPH_SERIALIZER.deserialize(graphJson);
        } catch (IOException e) {
            throw new KernelException("failed to deserialize workflow graph from traversal snapshot", e);
        }
    }
}
