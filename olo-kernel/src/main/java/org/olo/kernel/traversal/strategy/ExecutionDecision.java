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
        END
    }

    private final Kind kind;
    private final String strategyName;
    private final String nextNodeId;
    private final List<String> branchEntryNodeIds;
    private final String joinNodeId;

    private ExecutionDecision(
            Kind kind,
            String strategyName,
            String nextNodeId,
            List<String> branchEntryNodeIds,
            String joinNodeId) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName");
        this.nextNodeId = nextNodeId;
        this.branchEntryNodeIds =
                branchEntryNodeIds == null ? List.of() : List.copyOf(branchEntryNodeIds);
        this.joinNodeId = joinNodeId;
    }

    public static ExecutionDecision linear(String strategyName, String nextNodeId) {
        return new ExecutionDecision(Kind.LINEAR, strategyName, nextNodeId, List.of(), null);
    }

    public static ExecutionDecision end(String strategyName) {
        return new ExecutionDecision(Kind.END, strategyName, null, List.of(), null);
    }

    public static ExecutionDecision parallelFork(
            String strategyName, List<String> branchEntryNodeIds, String joinNodeId) {
        return new ExecutionDecision(
                Kind.PARALLEL_FORK,
                strategyName,
                null,
                branchEntryNodeIds,
                joinNodeId);
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
}
