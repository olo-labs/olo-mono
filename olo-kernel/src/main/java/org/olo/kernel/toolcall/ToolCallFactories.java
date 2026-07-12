/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.olo.kernel.toolcall.impl.DefaultAgentCallResultsStore;
import org.olo.kernel.toolcall.impl.DefaultToolCallRetryVariables;
import org.olo.kernel.toolcall.impl.DefaultToolCallSubgraphMerger;

/**
 * Default wiring for tool-call expansion collaborators used by {@link org.olo.kernel.traversal.factory.GraphTraverserFactory}.
 */
public final class ToolCallFactories {

    private ToolCallFactories() {
    }

    public static ToolCallSubgraphMerger defaultToolCallSubgraphMerger() {
        return new DefaultToolCallSubgraphMerger();
    }

    public static ToolCallRetryVariables defaultToolCallRetryVariables() {
        return new DefaultToolCallRetryVariables();
    }

    public static AgentCallResultsStore defaultAgentCallResultsStore() {
        return new DefaultAgentCallResultsStore();
    }
}
