/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.node;

import org.olo.spi.node.Node;

import java.util.List;

/**
 * Factory for built-in node implementations.
 */
public final class CoreNodes {

    private CoreNodes() {
    }

    public static List<Node> all() {
        return List.of(
                new PromptNode(),
                new AgentNode(),
                new ParallelNode(),
                new LoopNode(),
                new SwitchNode(),
                new ApprovalNode());
    }
}
