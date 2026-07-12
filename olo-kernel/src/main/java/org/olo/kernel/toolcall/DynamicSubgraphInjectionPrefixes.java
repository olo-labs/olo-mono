/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

public final class DynamicSubgraphInjectionPrefixes {

    public static final String AGENT_DYNAMIC_PREFIX = "agent-dyn-";
    public static final String TOOL_DYNAMIC_PREFIX = "tool-dyn-";

    private DynamicSubgraphInjectionPrefixes() {
    }

    public static boolean isInjectedToolNodeId(String nodeId) {
        return nodeId != null && nodeId.startsWith(TOOL_DYNAMIC_PREFIX);
    }

    public static boolean isInjectedNodeId(String nodeId) {
        return nodeId != null
                && (nodeId.startsWith(AGENT_DYNAMIC_PREFIX) || nodeId.startsWith(TOOL_DYNAMIC_PREFIX));
    }
}
