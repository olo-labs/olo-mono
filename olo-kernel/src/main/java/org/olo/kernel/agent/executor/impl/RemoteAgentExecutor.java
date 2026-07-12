/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.executor.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.spi.node.NodeResult;

/**
 * Agent that delegates to an external HTTP/MCP service.
 */
public final class RemoteAgentExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "remote";

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
        throw new KernelException("RemoteAgentExecutor is not implemented for node: " + node.getId());
    }
}
