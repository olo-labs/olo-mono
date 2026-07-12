/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.engine.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.start.StartNodeResolver;
import org.olo.kernel.graph.validate.GraphReadinessValidator;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.TraversalResult;
import org.olo.kernel.traversal.engine.GraphTraversalEngine;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.step.TraversalStepExecutor;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRegistry;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRequest;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.Objects;

public final class DefaultGraphTraversalEngine implements GraphTraversalEngine {

    private final StartNodeResolver startNodeResolver;
    private final GraphReadinessValidator readinessValidator;
    private final TraversalStepExecutor stepExecutor;
    private final ExecutionStrategyRegistry executionStrategyRegistry;

    public DefaultGraphTraversalEngine(
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

    @Override
    public KernelExecutionSnapshot executeSingleStep(KernelExecutionSnapshot snapshot) {
        KernelRuntimeContext context = snapshot.toContext();
        MutableGraphSession graphSession = new MutableGraphSession(context.getGraph());
        TraversalCursor cursor = TraversalCursor.fromSnapshot(snapshot);
        TraversalCursor updated = executeSingleStep(context, graphSession, cursor);
        return TraversalSnapshotMapper.toSnapshot(context, graphSession, updated);
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
        TraversalDiagnostics.logStepEnter(step, node, context.getVariableMap());

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

        if (decision.kind() == ExecutionDecision.Kind.PARALLEL_FORK) {
            return ParallelBranchExecutor.execute(
                    context,
                    graphSession,
                    decision,
                    step,
                    currentNodeId,
                    result.message(),
                    this::executeSingleStep);
        }

        return ExecutionDecisionCursorApplier.apply(
                context, node, step, currentNodeId, decision, result.message());
    }
}
