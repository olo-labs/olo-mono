package org.olo.kernel.traversal.engine;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.start.StartNodeResolver;
import org.olo.kernel.graph.validate.GraphReadinessValidator;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.TraversalResult;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.step.TraversalStepExecutor;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRegistry;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.List;
import java.util.Objects;

/**
 * Step-based graph traversal used by synchronous {@link org.olo.kernel.traversal.GraphTraverser}
 * and Temporal per-node activities.
 */
public final class GraphTraversalEngine {

    private final StartNodeResolver startNodeResolver;
    private final GraphReadinessValidator readinessValidator;
    private final TraversalStepExecutor stepExecutor;
    private final ExecutionStrategyRegistry executionStrategyRegistry;

    public GraphTraversalEngine(
            StartNodeResolver startNodeResolver,
            GraphReadinessValidator readinessValidator,
            TraversalStepExecutor stepExecutor,
            ExecutionStrategyRegistry executionStrategyRegistry) {
        this.startNodeResolver = Objects.requireNonNull(startNodeResolver, "startNodeResolver");
        this.readinessValidator = Objects.requireNonNull(readinessValidator, "readinessValidator");
        this.stepExecutor = Objects.requireNonNull(stepExecutor, "stepExecutor");
        this.executionStrategyRegistry =
                Objects.requireNonNull(executionStrategyRegistry, "executionStrategyRegistry");
    }

    public TraversalResult traverse(KernelRuntimeContext context) {
        MutableGraphSession graphSession = new MutableGraphSession(context.getGraph());
        if (!readinessValidator.isReady(graphSession.index())) {
            throw new KernelException("workflow graph is not ready for queue: " + context.getQueue());
        }

        NodeDefinition start = startNodeResolver.resolve(graphSession.index())
                .orElseThrow(() -> new KernelException("workflow graph has no START node"));
        TraversalDiagnostics.logTraversalStart(
                context,
                graphSession.index().nodes().size(),
                graphSession.graph().getEdges().size(),
                start.getId());

        TraversalCursor cursor = TraversalCursor.running(null, 0, null, null);
        while (cursor.status() == KernelExecutionSnapshot.Status.RUNNING) {
            cursor = executeSingleStep(context, graphSession, cursor);
        }

        if (cursor.status() == KernelExecutionSnapshot.Status.FAILED) {
            return TraversalResult.failed(cursor.lastNodeId(), cursor.lastStatus(), cursor.message());
        }

        TraversalDiagnostics.logTraversalComplete(
                cursor.lastNodeId(), cursor.message(), context.getVariableMap(), context.getOutputMap());
        return TraversalResult.completed(cursor.lastNodeId(), cursor.message());
    }

    public KernelExecutionSnapshot executeSingleStep(KernelExecutionSnapshot snapshot) {
        KernelRuntimeContext context = snapshot.toContext();
        MutableGraphSession graphSession = new MutableGraphSession(context.getGraph());
        TraversalCursor cursor = TraversalCursor.fromSnapshot(snapshot);
        TraversalCursor updated = executeSingleStep(context, graphSession, cursor);
        return toSnapshot(context, graphSession, updated);
    }

    private TraversalCursor executeSingleStep(
            KernelRuntimeContext context, MutableGraphSession graphSession, TraversalCursor cursor) {
        if (cursor.status() != KernelExecutionSnapshot.Status.RUNNING) {
            return cursor;
        }

        GraphIndex index = graphSession.index();
        String currentNodeId = cursor.resolveNextNodeId(index);
        int step = cursor.step() + 1;

        NodeDefinition node = index.findNode(currentNodeId)
                .orElseThrow(() -> new KernelException("workflow graph node not found: " + currentNodeId));
        TraversalDiagnostics.logStepEnter(step, node.getId(), node.getType(), context.getVariableMap());

        NodeResult result = stepExecutor.execute(context, node, step);
        if (result.status() == NodeStatus.FAILED) {
            String message = result.message() != null ? result.message() : "node execution failed";
            TraversalDiagnostics.logTraversalFailed(currentNodeId, result.status(), message);
            return TraversalCursor.failed(currentNodeId, step, result.status(), message);
        }
        if (result.status() == NodeStatus.WAITING) {
            String message = result.message() != null ? result.message() : "node execution waiting";
            TraversalDiagnostics.logTraversalFailed(currentNodeId, result.status(), message);
            return TraversalCursor.failed(currentNodeId, step, result.status(), message);
        }

        ExecutionDecision decision = executionStrategyRegistry.decide(
                new ExecutionStrategyRequest(context, graphSession.index(), graphSession, node, result, step));
        TraversalDiagnostics.logExecutionDecision(node.getId(), decision);

        if (decision.kind() == ExecutionDecision.Kind.FAILED) {
            String message = decision.failureMessage().orElse("dynamic graph expansion failed");
            TraversalDiagnostics.logTraversalFailed(currentNodeId, NodeStatus.FAILED, message);
            return TraversalCursor.failed(currentNodeId, step, NodeStatus.FAILED, message);
        }
        if (decision.kind() == ExecutionDecision.Kind.REEXECUTE) {
            String reexecuteNodeId = decision.nextNodeId().orElse(currentNodeId);
            TraversalDiagnostics.logStepExit(step, node.getId(), reexecuteNodeId, context.getVariableMap());
            return TraversalCursor.running(reexecuteNodeId, step - 1, currentNodeId, result.message());
        }
        if (decision.kind() == ExecutionDecision.Kind.EXPAND_SUBGRAPH) {
            String expandedNodeId = decision.nextNodeId()
                    .orElseThrow(() -> new KernelException("dynamic subgraph expansion produced no entry node"));
            TraversalDiagnostics.logStepExit(step, node.getId(), expandedNodeId, context.getVariableMap());
            return TraversalCursor.running(expandedNodeId, step, currentNodeId, result.message());
        }
        if (decision.kind() == ExecutionDecision.Kind.PARALLEL_FORK) {
            return executeParallelFork(context, graphSession, cursor, decision, step, currentNodeId, result.message());
        }

        if (decision.kind() == ExecutionDecision.Kind.END || decision.nextNodeId().isEmpty()) {
            TraversalDiagnostics.logStepExit(step, node.getId(), null, context.getVariableMap());
            return TraversalCursor.completed(currentNodeId, step, result.message());
        }

        String nextNodeId = decision.nextNodeId().get();
        TraversalDiagnostics.logStepExit(step, node.getId(), nextNodeId, context.getVariableMap());
        return TraversalCursor.running(nextNodeId, step, currentNodeId, result.message());
    }

    private TraversalCursor executeParallelFork(
            KernelRuntimeContext context,
            MutableGraphSession graphSession,
            TraversalCursor cursor,
            ExecutionDecision decision,
            int step,
            String forkNodeId,
            String forkMessage) {
        List<String> branchEntryNodeIds = decision.branchEntryNodeIds();
        if (branchEntryNodeIds.isEmpty()) {
            throw new KernelException("PARALLEL fork has no outgoing branches");
        }
        String joinNodeId = decision.joinNodeId()
                .orElseThrow(() -> new KernelException(
                        "PARALLEL fork could not resolve a common join node for branches: "
                                + branchEntryNodeIds));

        int branchStep = step;
        for (String branchStart : branchEntryNodeIds) {
            TraversalCursor branchCursor = TraversalCursor.running(branchStart, branchStep, forkNodeId, forkMessage);
            while (branchCursor.status() == KernelExecutionSnapshot.Status.RUNNING
                    && !joinNodeId.equals(branchCursor.resolveNextNodeId(graphSession.index()))) {
                branchCursor = executeSingleStep(context, graphSession, branchCursor);
            }
            if (branchCursor.status() != KernelExecutionSnapshot.Status.RUNNING) {
                return branchCursor;
            }
            branchStep = branchCursor.step();
        }

        return TraversalCursor.running(joinNodeId, branchStep, forkNodeId, forkMessage);
    }

    private static KernelExecutionSnapshot toSnapshot(
            KernelRuntimeContext context, MutableGraphSession graphSession, TraversalCursor cursor) {
        KernelRuntimeContext refreshed = refreshedContext(context, graphSession);
        return KernelExecutionSnapshot.fromContext(
                refreshed,
                cursor.nextNodeId(),
                cursor.step(),
                cursor.status(),
                cursor.lastNodeId(),
                cursor.lastStatus(),
                cursor.message());
    }

    private static KernelRuntimeContext refreshedContext(
            KernelRuntimeContext context, MutableGraphSession graphSession) {
        return new KernelRuntimeContext(
                context.getQueue(),
                context.getInput(),
                graphSession.graph(),
                true,
                context.getVariables(),
                context.getOutputs());
    }

    private record TraversalCursor(
            String nextNodeId,
            int step,
            KernelExecutionSnapshot.Status status,
            String lastNodeId,
            NodeStatus lastStatus,
            String message) {

        static TraversalCursor running(String nextNodeId, int step, String lastNodeId, String message) {
            return new TraversalCursor(
                    nextNodeId, step, KernelExecutionSnapshot.Status.RUNNING, lastNodeId, NodeStatus.COMPLETED, message);
        }

        static TraversalCursor completed(String lastNodeId, int step, String message) {
            return new TraversalCursor(
                    null, step, KernelExecutionSnapshot.Status.COMPLETED, lastNodeId, NodeStatus.COMPLETED, message);
        }

        static TraversalCursor failed(String lastNodeId, int step, NodeStatus lastStatus, String message) {
            return new TraversalCursor(
                    null, step, KernelExecutionSnapshot.Status.FAILED, lastNodeId, lastStatus, message);
        }

        static TraversalCursor fromSnapshot(KernelExecutionSnapshot snapshot) {
            return new TraversalCursor(
                    snapshot.getNextNodeId(),
                    snapshot.getStep(),
                    snapshot.getStatus(),
                    snapshot.getLastNodeId(),
                    snapshot.getLastStatus(),
                    snapshot.getMessage());
        }

        String resolveNextNodeId(GraphIndex index) {
            if (nextNodeId != null && !nextNodeId.isBlank()) {
                return nextNodeId;
            }
            return index.nodes().stream()
                    .filter(node -> "START".equals(node.getType()))
                    .map(NodeDefinition::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("workflow graph has no START node"));
        }
    }
}
