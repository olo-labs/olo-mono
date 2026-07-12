/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.strategy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestration outcome after a node completes: where the graph walk continues next.
 */
public final class ExecutionDecision {

    public enum Kind {
        /** Follow a single next node id (including end of graph when absent). */
        LINEAR,
        /** Fan out into parallel branches that must converge before continuing. */
        PARALLEL_FORK,
        /** Graph walk is complete. */
        END,
        /** Re-run the same node (for example invalid structured output retry). */
        REEXECUTE,
        /** Continue into a dynamically merged subgraph entry node. */
        EXPAND_SUBGRAPH,
        /** Stop traversal with a failure message. */
        FAILED
    }

    private final Kind kind;
    private final String strategyName;
    private final String nextNodeId;
    private final List<String> branchEntryNodeIds;
    private final String joinNodeId;
    private final String failureMessage;

    private ExecutionDecision(
            Kind kind,
            String strategyName,
            String nextNodeId,
            List<String> branchEntryNodeIds,
            String joinNodeId,
            String failureMessage) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName");
        this.nextNodeId = nextNodeId;
        this.branchEntryNodeIds =
                branchEntryNodeIds == null ? List.of() : List.copyOf(branchEntryNodeIds);
        this.joinNodeId = joinNodeId;
        this.failureMessage = failureMessage;
    }

    public static ExecutionDecision linear(String strategyName, String nextNodeId) {
        return new ExecutionDecision(Kind.LINEAR, strategyName, nextNodeId, List.of(), null, null);
    }

    public static ExecutionDecision end(String strategyName) {
        return new ExecutionDecision(Kind.END, strategyName, null, List.of(), null, null);
    }

    public static ExecutionDecision parallelFork(
            String strategyName, List<String> branchEntryNodeIds, String joinNodeId) {
        return new ExecutionDecision(
                Kind.PARALLEL_FORK,
                strategyName,
                null,
                branchEntryNodeIds,
                joinNodeId,
                null);
    }

    public static ExecutionDecision reexecute(String strategyName, String nodeId) {
        return new ExecutionDecision(Kind.REEXECUTE, strategyName, nodeId, List.of(), null, null);
    }

    public static ExecutionDecision expandSubgraph(String strategyName, String entryNodeId) {
        return new ExecutionDecision(Kind.EXPAND_SUBGRAPH, strategyName, entryNodeId, List.of(), null, null);
    }

    public static ExecutionDecision failed(String strategyName, String failureMessage) {
        return new ExecutionDecision(Kind.FAILED, strategyName, null, List.of(), null, failureMessage);
    }

    public Kind kind() {
        return kind;
    }

    public String strategyName() {
        return strategyName;
    }

    public Optional<String> nextNodeId() {
        return Optional.ofNullable(nextNodeId).filter(id -> !id.isBlank());
    }

    public List<String> branchEntryNodeIds() {
        return branchEntryNodeIds;
    }

    public Optional<String> joinNodeId() {
        return Optional.ofNullable(joinNodeId).filter(id -> !id.isBlank());
    }

    public Optional<String> failureMessage() {
        return Optional.ofNullable(failureMessage).filter(message -> !message.isBlank());
    }
}
