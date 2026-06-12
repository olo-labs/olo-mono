package org.olo.kernel.agent.executor;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeResult;

/**
 * Executes an {@code AGENT} canvas node for a specific orchestration backend
 * (local LLM, child workflow, human gate, remote service, …).
 */
public interface AgentExecutor {

    /** Stable id for logs and {@link org.olo.spi.node.NodeResult} output metadata. */
    String id();

    /**
     * Whether this executor handles the given node definition.
     * Registry order matters: specific executors first, local LLM fallback last.
     */
    boolean supports(NodeDefinition node);

    NodeResult execute(KernelRuntimeContext context, NodeDefinition node);
}
