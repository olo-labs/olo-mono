package org.olo.kernel.traversal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.ExecutionOutputs;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;
import org.olo.kernel.traversal.scheduling.NodeExecutionScheduling;
import org.olo.spi.node.NodeStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable traversal state passed between Temporal activities.
 * The workflow graph is stored as JSON to avoid Temporal's default Jackson mapper
 * deserializing {@link WorkflowDefinition} builder types incorrectly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class KernelExecutionSnapshot {

    private static final JsonWorkflowSerializer GRAPH_SERIALIZER = new JsonWorkflowSerializer();

    public enum Status {
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final String queue;
    private final WorkflowInput input;
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
        this.graphJson = Objects.requireNonNull(graphJson, "graphJson");
        this.variables = copyMapAllowingNullValues(variables);
        this.outputs = copyMapAllowingNullValues(outputs);
        this.nextNodeId = nextNodeId;
        this.step = step;
        this.status = Objects.requireNonNull(status, "status");
        this.lastNodeId = lastNodeId;
        this.lastStatus = lastStatus;
        this.message = message;
        WorkflowDefinition graph = readGraph(this.graphJson);
        this.nextRequiresDedicatedActivity = nextRequiresDedicatedActivity != null
                ? nextRequiresDedicatedActivity
                : computeNextRequiresDedicatedActivity(graph, nextNodeId, status);
        this.workflowActivityName = workflowActivityName != null && !workflowActivityName.isBlank()
                ? workflowActivityName
                : NodeActivityNaming.formatWorkflow(graph);
        this.nextActivityName = nextActivityName != null
                ? nextActivityName
                : computeNextActivityName(graph, nextNodeId, status);
    }

    public static KernelExecutionSnapshot fromContext(KernelRuntimeContext context) {
        return fromContext(context, null, 0, Status.RUNNING, null, null, null);
    }

    public static KernelExecutionSnapshot fromContext(
            KernelRuntimeContext context,
            String nextNodeId,
            int step,
            Status status,
            String lastNodeId,
            NodeStatus lastStatus,
            String message) {
        WorkflowDefinition graph = context.getGraph();
        return new KernelExecutionSnapshot(
                context.getQueue(),
                context.getInput(),
                serializeGraph(graph),
                context.getVariableMap(),
                context.getOutputMap(),
                nextNodeId,
                step,
                status,
                lastNodeId,
                lastStatus,
                message,
                computeNextRequiresDedicatedActivity(graph, nextNodeId, status),
                NodeActivityNaming.formatWorkflow(graph),
                computeNextActivityName(graph, nextNodeId, status));
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

    @JsonIgnore
    public WorkflowDefinition getGraph() {
        return readGraph(graphJson);
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
            case RUNNING -> throw new IllegalStateException("traversal snapshot is still running");
        };
    }

    public String getQueue() {
        return queue;
    }

    public WorkflowInput getInput() {
        return input;
    }

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

    static boolean computeNextRequiresDedicatedActivity(
            WorkflowDefinition graph, String nextNodeId, Status status) {
        if (status != Status.RUNNING) {
            return false;
        }
        GraphIndex index = new DefaultGraphIndex(graph);
        String resolvedNextNodeId = nextNodeId;
        if (resolvedNextNodeId == null || resolvedNextNodeId.isBlank()) {
            resolvedNextNodeId = index.nodes().stream()
                    .filter(node -> "START".equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (resolvedNextNodeId == null) {
            return true;
        }
        final String lookupNodeId = resolvedNextNodeId;
        return index.findNode(lookupNodeId)
                .map(NodeExecutionScheduling::requiresDedicatedActivity)
                .orElse(true);
    }

    static String computeNextActivityName(WorkflowDefinition graph, String nextNodeId, Status status) {
        if (status != Status.RUNNING) {
            return null;
        }
        GraphIndex index = new DefaultGraphIndex(graph);
        String resolvedNextNodeId = nextNodeId;
        if (resolvedNextNodeId == null || resolvedNextNodeId.isBlank()) {
            resolvedNextNodeId = index.nodes().stream()
                    .filter(node -> "START".equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (resolvedNextNodeId == null) {
            return null;
        }
        final String lookupNodeId = resolvedNextNodeId;
        return index.findNode(lookupNodeId).map(NodeActivityNaming::formatNode).orElse(null);
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

    private static <K, V> Map<K, V> copyMapAllowingNullValues(Map<K, V> source) {
        Objects.requireNonNull(source, "source");
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
