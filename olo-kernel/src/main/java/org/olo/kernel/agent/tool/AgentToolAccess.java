/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.olo.toolgate.client.ToolGateClient;

import java.util.Optional;

/**
 * Worker-wide access to OLO ToolGate. When disabled, agents run without tools exactly as before.
 *
 * @param client               the ToolGate client, or {@code null} when disabled
 * @param defaultMaxToolRounds tool rounds per agent turn when the AGENT node does not set {@code maxToolRounds}
 */
public record AgentToolAccess(ToolGateClient client, int defaultMaxToolRounds) {

    private static final AgentToolAccess DISABLED = new AgentToolAccess(null, 0);

    public AgentToolAccess {
        if (defaultMaxToolRounds < 0) {
            throw new IllegalArgumentException("defaultMaxToolRounds must not be negative");
        }
    }

    public static AgentToolAccess disabled() {
        return DISABLED;
    }

    public static AgentToolAccess enabled(ToolGateClient client, int defaultMaxToolRounds) {
        return new AgentToolAccess(java.util.Objects.requireNonNull(client, "client"), defaultMaxToolRounds);
    }

    public boolean isEnabled() {
        return client != null;
    }

    public Optional<ToolGateClient> toolGate() {
        return Optional.ofNullable(client);
    }
}
