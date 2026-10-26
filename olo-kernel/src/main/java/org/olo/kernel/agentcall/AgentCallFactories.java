/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agentcall;

import org.olo.kernel.agentcall.impl.DefaultAgentCallResultsStore;
import org.olo.kernel.agentcall.impl.DefaultAgentCallRetryVariables;
import org.olo.kernel.agentcall.impl.DefaultAgentCallSubgraphMerger;

/**
 * Default wiring for tool-call expansion collaborators used by {@link org.olo.kernel.traversal.factory.GraphTraverserFactory}.
 */
public final class AgentCallFactories {

    private AgentCallFactories() {
    }

    public static AgentCallSubgraphMerger defaultAgentCallSubgraphMerger() {
        return new DefaultAgentCallSubgraphMerger();
    }

    public static AgentCallRetryVariables defaultAgentCallRetryVariables() {
        return new DefaultAgentCallRetryVariables();
    }

    public static AgentCallResultsStore defaultAgentCallResultsStore() {
        return new DefaultAgentCallResultsStore();
    }
}
