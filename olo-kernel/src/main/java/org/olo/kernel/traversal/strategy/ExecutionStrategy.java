package org.olo.kernel.traversal.strategy;

/**
 * Orchestrates graph navigation after a node executes. Node execution itself is handled by
 * {@link org.olo.kernel.traversal.step.TraversalStepExecutor} / {@link org.olo.kernel.traversal.step.handler.NodeTypeHandler}.
 * <p>
 * Examples: linear chains, conditional branches, parallel fan-out/join, child-workflow resume.
 */
public interface ExecutionStrategy {

    String name();

    /**
     * Whether this strategy owns navigation immediately after {@code request.completedNode()} completes.
     */
    boolean supports(ExecutionStrategyRequest request);

    /**
     * Decides the next orchestration step for the traverser.
     */
    ExecutionDecision decide(ExecutionStrategyRequest request);
}
