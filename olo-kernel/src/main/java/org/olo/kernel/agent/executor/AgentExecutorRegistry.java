package org.olo.kernel.agent.executor;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.exception.KernelException;

import java.util.List;
import java.util.Objects;

/**
 * Selects the first {@link AgentExecutor} that supports an AGENT node.
 */
public final class AgentExecutorRegistry {

    private final List<AgentExecutor> executors;

    public AgentExecutorRegistry(List<AgentExecutor> executors) {
        this.executors = List.copyOf(Objects.requireNonNull(executors, "executors"));
        if (this.executors.isEmpty()) {
            throw new KernelException("agent executor registry requires at least one executor");
        }
    }

    public AgentExecutor resolve(NodeDefinition node) {
        Objects.requireNonNull(node, "node");
        for (AgentExecutor executor : executors) {
            if (executor.supports(node)) {
                return executor;
            }
        }
        throw new KernelException(
                "no agent executor registered for node: " + node.getId() + " type=" + node.getType());
    }
}
