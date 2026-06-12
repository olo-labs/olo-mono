package org.olo.kernel.traversal.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.graph.start.StartNodeResolver;
import org.olo.kernel.graph.validate.GraphReadinessValidator;
import org.olo.kernel.traversal.GraphTraverser;
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
 * Walks the workflow graph: executes nodes via {@link TraversalStepExecutor}, then delegates
 * orchestration to {@link ExecutionStrategyRegistry}.
 */
public final class DefaultGraphTraverser implements GraphTraverser {

    private final StartNodeResolver startNodeResolver;
    private final GraphReadinessValidator readinessValidator;
    private final TraversalStepExecutor stepExecutor;
    private final ExecutionStrategyRegistry executionStrategyRegistry;

    public DefaultGraphTraverser(
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

    @Override
    public TraversalResult traverse(KernelRuntimeContext context) {
        Objects.requireNonNull(context, "context");
        GraphIndex index = new DefaultGraphIndex(context.getGraph());
        if (!readinessValidator.isReady(index)) {
            throw new KernelException("workflow graph is not ready for queue: " + context.getQueue());
        }

        NodeDefinition start = startNodeResolver.resolve(index)
                .orElseThrow(() -> new KernelException("workflow graph has no START node"));
        TraversalDiagnostics.logTraversalStart(
                context,
                index.nodes().size(),
                context.getGraph().getEdges().size(),
                start.getId());

        WalkState walk = walkFrom(context, index, start.getId(), 0);
        if (walk.failed()) {
            return TraversalResult.failed(walk.lastNodeId(), walk.lastStatus(), walk.failureMessage());
        }

        TraversalDiagnostics.logTraversalComplete(
                walk.lastNodeId(), walk.lastMessage(), context.getVariableMap(), context.getOutputMap());
        return TraversalResult.completed(walk.lastNodeId(), walk.lastMessage());
    }

    private WalkState walkFrom(KernelRuntimeContext context, GraphIndex index, String startNodeId, int step) {
        String currentNodeId = startNodeId;
        String lastNodeId = startNodeId;
        NodeResult lastResult = null;

        while (currentNodeId != null) {
            step++;
            final String activeNodeId = currentNodeId;
            NodeDefinition node = index.findNode(activeNodeId)
                    .orElseThrow(() -> new KernelException("workflow graph node not found: " + activeNodeId));
            TraversalDiagnostics.logStepEnter(step, node.getId(), node.getType(), context.getVariableMap());

            lastNodeId = currentNodeId;
            lastResult = stepExecutor.execute(context, node, step);
            if (lastResult.status() == NodeStatus.FAILED) {
                String message = lastResult.message() != null ? lastResult.message() : "node execution failed";
                TraversalDiagnostics.logTraversalFailed(currentNodeId, lastResult.status(), message);
                return WalkState.failed(currentNodeId, lastResult.status(), message);
            }
            if (lastResult.status() == NodeStatus.WAITING) {
                String message = lastResult.message() != null ? lastResult.message() : "node execution waiting";
                TraversalDiagnostics.logTraversalFailed(currentNodeId, lastResult.status(), message);
                return WalkState.failed(currentNodeId, lastResult.status(), message);
            }

            ExecutionStrategyRequest strategyRequest =
                    new ExecutionStrategyRequest(context, index, node, lastResult, step);
            ExecutionDecision decision = executionStrategyRegistry.decide(strategyRequest);
            TraversalDiagnostics.logExecutionDecision(node.getId(), decision);

            if (decision.kind() == ExecutionDecision.Kind.PARALLEL_FORK) {
                return executeParallelFork(context, index, decision, step);
            }

            if (decision.kind() == ExecutionDecision.Kind.END || decision.nextNodeId().isEmpty()) {
                TraversalDiagnostics.logStepExit(step, node.getId(), null, context.getVariableMap());
                currentNodeId = null;
            } else {
                String nextNodeId = decision.nextNodeId().get();
                TraversalDiagnostics.logStepExit(step, node.getId(), nextNodeId, context.getVariableMap());
                currentNodeId = nextNodeId;
            }
        }

        String message = lastResult != null ? lastResult.message() : null;
        return WalkState.completed(lastNodeId, message, lastResult, step);
    }

    private WalkState executeParallelFork(
            KernelRuntimeContext context, GraphIndex index, ExecutionDecision decision, int step) {
        List<String> branchEntryNodeIds = decision.branchEntryNodeIds();
        if (branchEntryNodeIds.isEmpty()) {
            throw new KernelException("PARALLEL fork has no outgoing branches");
        }
        String joinNodeId = decision.joinNodeId()
                .orElseThrow(() -> new KernelException(
                        "PARALLEL fork could not resolve a common join node for branches: "
                                + branchEntryNodeIds));

        for (String branchStart : branchEntryNodeIds) {
            WalkState branchWalk = walkBranchUntilJoin(context, index, branchStart, joinNodeId, step);
            if (branchWalk.failed()) {
                return branchWalk;
            }
            step = branchWalk.step();
        }

        return walkFrom(context, index, joinNodeId, step);
    }

    private WalkState walkBranchUntilJoin(
            KernelRuntimeContext context, GraphIndex index, String branchStartId, String joinNodeId, int step) {
        String currentNodeId = branchStartId;
        String lastNodeId = branchStartId;
        NodeResult lastResult = null;

        while (currentNodeId != null && !currentNodeId.equals(joinNodeId)) {
            step++;
            final String activeNodeId = currentNodeId;
            NodeDefinition node = index.findNode(activeNodeId)
                    .orElseThrow(() -> new KernelException("workflow graph node not found: " + activeNodeId));
            TraversalDiagnostics.logStepEnter(step, node.getId(), node.getType(), context.getVariableMap());

            lastNodeId = currentNodeId;
            lastResult = stepExecutor.execute(context, node, step);
            if (lastResult.status() == NodeStatus.FAILED) {
                String message = lastResult.message() != null ? lastResult.message() : "node execution failed";
                TraversalDiagnostics.logTraversalFailed(currentNodeId, lastResult.status(), message);
                return WalkState.failed(currentNodeId, lastResult.status(), message);
            }
            if (lastResult.status() == NodeStatus.WAITING) {
                String message = lastResult.message() != null ? lastResult.message() : "node execution waiting";
                TraversalDiagnostics.logTraversalFailed(currentNodeId, lastResult.status(), message);
                return WalkState.failed(currentNodeId, lastResult.status(), message);
            }

            ExecutionDecision decision = executionStrategyRegistry.decide(
                    new ExecutionStrategyRequest(context, index, node, lastResult, step));
            TraversalDiagnostics.logExecutionDecision(node.getId(), decision);

            if (decision.kind() == ExecutionDecision.Kind.PARALLEL_FORK) {
                throw new KernelException("nested PARALLEL forks are not supported yet at node: " + node.getId());
            }
            if (decision.kind() == ExecutionDecision.Kind.END || decision.nextNodeId().isEmpty()) {
                TraversalDiagnostics.logStepExit(step, node.getId(), null, context.getVariableMap());
                currentNodeId = null;
            } else {
                String nextNodeId = decision.nextNodeId().get();
                TraversalDiagnostics.logStepExit(step, node.getId(), nextNodeId, context.getVariableMap());
                currentNodeId = nextNodeId;
            }
        }

        return WalkState.completed(lastNodeId, lastResult != null ? lastResult.message() : null, lastResult, step);
    }

    private record WalkState(
            boolean failed,
            String lastNodeId,
            NodeStatus lastStatus,
            String failureMessage,
            String lastMessage,
            NodeResult lastResult,
            int step) {

        static WalkState failed(String lastNodeId, NodeStatus status, String message) {
            return new WalkState(true, lastNodeId, status, message, null, null, 0);
        }

        static WalkState completed(String lastNodeId, String lastMessage, NodeResult lastResult, int step) {
            return new WalkState(false, lastNodeId, NodeStatus.COMPLETED, null, lastMessage, lastResult, step);
        }
    }
}
