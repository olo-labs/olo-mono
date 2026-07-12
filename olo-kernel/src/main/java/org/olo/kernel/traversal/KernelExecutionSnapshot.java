/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.ExecutionOutputs;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;
import org.olo.kernel.traversal.snapshot.impl.GraphSnapshotPolicy;
import org.olo.kernel.traversal.snapshot.impl.KernelExecutionSnapshotFactory;
import org.olo.kernel.traversal.snapshot.impl.SnapshotMapSupport;
import org.olo.kernel.traversal.snapshot.impl.SnapshotSchedulingResolver;
import org.olo.spi.node.NodeStatus;

import java.util.Map;
import java.util.Objects;

/**
 * Serializable traversal state passed between Temporal activities.
 * The workflow graph is resolved from the worker registry when unchanged; {@code graphJson}
 * is embedded only after inline dynamic graph expansion mutates the graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class KernelExecutionSnapshot {

    public enum Status {
        RUNNING,
        WAITING,
        COMPLETED,
        FAILED
    }

    private final String queue;
    private final WorkflowInput input;
    @JsonIgnore
    private final WorkflowDefinition graph;
    private final String graphJson;
    private final Map<String, Object> variables;
    private final Map<String, ExecutionOutput> outputs;
    private final String nextNodeId;
    private final int step;
    private final Status status;
    private final String lastNodeId;
    private final NodeStatus lastStatus;
    private final String message;
    private final boolean nextRequiresDedicatedActivity;
    private final String workflowActivityName;
    private final String nextActivityName;

    @JsonCreator
    public KernelExecutionSnapshot(
            @JsonProperty("queue") String queue,
            @JsonProperty("input") WorkflowInput input,
            @JsonProperty("graphJson") String graphJson,
            @JsonProperty("variables") Map<String, Object> variables,
            @JsonProperty("outputs") Map<String, ExecutionOutput> outputs,
            @JsonProperty("nextNodeId") String nextNodeId,
            @JsonProperty("step") int step,
            @JsonProperty("status") Status status,
            @JsonProperty("lastNodeId") String lastNodeId,
            @JsonProperty("lastStatus") NodeStatus lastStatus,
            @JsonProperty("message") String message,
            @JsonProperty("nextRequiresDedicatedActivity") Boolean nextRequiresDedicatedActivity,
            @JsonProperty("workflowActivityName") String workflowActivityName,
            @JsonProperty("nextActivityName") String nextActivityName) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.input = Objects.requireNonNull(input, "input");
        this.graph = GraphSnapshotPolicy.resolveGraph(queue, input, graphJson);
        this.graphJson = graphJson;
        this.variables = SnapshotMapSupport.copyMapAllowingNullValues(variables);
        this.outputs = SnapshotMapSupport.copyMapAllowingNullValues(outputs);
        this.nextNodeId = nextNodeId;
        this.step = step;
        this.status = Objects.requireNonNull(status, "status");
        this.lastNodeId = lastNodeId;
        this.lastStatus = lastStatus;
        this.message = message;
        WorkflowDefinition resolvedGraph = this.graph;
        this.nextRequiresDedicatedActivity = nextRequiresDedicatedActivity != null
                ? nextRequiresDedicatedActivity
                : SnapshotSchedulingResolver.computeNextRequiresDedicatedActivity(resolvedGraph, nextNodeId, status);
        this.workflowActivityName = workflowActivityName != null && !workflowActivityName.isBlank()
                ? workflowActivityName
                : NodeActivityNaming.formatWorkflow(resolvedGraph);
        this.nextActivityName = nextActivityName != null
                ? nextActivityName
                : SnapshotSchedulingResolver.computeNextActivityName(resolvedGraph, nextNodeId, status);
    }

    public static KernelExecutionSnapshot fromContext(KernelRuntimeContext context) {
        return KernelExecutionSnapshotFactory.fromContext(context);
    }

    public static KernelExecutionSnapshot fromContext(
            KernelRuntimeContext context,
            String nextNodeId,
            int step,
            Status status,
            String lastNodeId,
            NodeStatus lastStatus,
            String message) {
        return KernelExecutionSnapshotFactory.fromContext(
                context, nextNodeId, step, status, lastNodeId, lastStatus, message);
    }

    public KernelRuntimeContext toContext() {
        return new KernelRuntimeContext(
                queue,
                input,
                getGraph(),
                true,
                WorkflowRuntimeVariables.fromMap(variables),
                ExecutionOutputs.fromMap(outputs));
    }

    public WorkflowDefinition getGraph() {
        return graph;
    }

    public GraphIndex graphIndex() {
        return new DefaultGraphIndex(getGraph());
    }

    public MutableGraphSession graphSession() {
        return new MutableGraphSession(getGraph());
    }

    public boolean isTerminal() {
        return status != Status.RUNNING;
    }

    public boolean isWaiting() {
        return status == Status.WAITING;
    }

    public boolean requiresDedicatedActivityForNextNode() {
        return nextRequiresDedicatedActivity;
    }

    public String resolveNextNodeId() {
        if (nextNodeId != null && !nextNodeId.isBlank()) {
            return nextNodeId;
        }
        return graphIndex()
                .nodes()
                .stream()
                .filter(node -> "START".equals(node.getType()))
                .map(NodeDefinition::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("workflow graph has no START node"));
    }

    public TraversalResult toTraversalResult() {
        return switch (status) {
            case COMPLETED -> TraversalResult.completed(lastNodeId, message);
            case FAILED -> TraversalResult.failed(
                    lastNodeId, lastStatus != null ? lastStatus : NodeStatus.FAILED, message);
            case WAITING -> TraversalResult.waiting(lastNodeId, message);
            case RUNNING -> throw new IllegalStateException("traversal snapshot is still running");
        };
    }

    public String getQueue() {
        return queue;
    }

    public WorkflowInput getInput() {
        return input;
    }

    @JsonProperty("graphJson")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getGraphJson() {
        return graphJson;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Map<String, ExecutionOutput> getOutputs() {
        return outputs;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public int getStep() {
        return step;
    }

    public Status getStatus() {
        return status;
    }

    public String getLastNodeId() {
        return lastNodeId;
    }

    public NodeStatus getLastStatus() {
        return lastStatus;
    }

    public String getMessage() {
        return message;
    }

    public boolean isNextRequiresDedicatedActivity() {
        return nextRequiresDedicatedActivity;
    }

    public String getWorkflowActivityName() {
        return workflowActivityName;
    }

    public String getNextActivityName() {
        return nextActivityName;
    }
}
