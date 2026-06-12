package org.olo.kernel.traversal.strategy;

import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.strategy.impl.LinearExecutionStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Selects the first {@link ExecutionStrategy} that supports a completed node.
 * {@link LinearExecutionStrategy} must remain last as the universal fallback.
 */
public final class ExecutionStrategyRegistry {

    private final List<ExecutionStrategy> strategies;

    public ExecutionStrategyRegistry(List<ExecutionStrategy> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "strategies"));
        if (this.strategies.isEmpty()) {
            throw new KernelException("execution strategy registry requires at least one strategy");
        }
        ExecutionStrategy last = this.strategies.get(this.strategies.size() - 1);
        if (!(last instanceof LinearExecutionStrategy)) {
            throw new KernelException(
                    "execution strategy registry must end with LinearExecutionStrategy as fallback");
        }
    }

    public ExecutionDecision decide(ExecutionStrategyRequest request) {
        for (ExecutionStrategy strategy : strategies) {
            if (strategy.supports(request)) {
                return strategy.decide(request);
            }
        }
        throw new KernelException(
                "no execution strategy matched node: " + request.completedNode().getId());
    }
}
