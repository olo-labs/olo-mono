package org.olo.kernel.agent.executor.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.spi.node.NodeResult;

/**
 * Agent that blocks on human input / approval. Distinct from canvas {@code HUMAN} nodes (handled via SPI).
 */
public final class HumanAgentExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "human";

    @Override
    public String id() {
        return EXECUTOR_ID;
    }

    @Override
    public boolean supports(NodeDefinition node) {
        return false;
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        throw new KernelException("HumanAgentExecutor is not implemented for node: " + node.getId());
    }
}
