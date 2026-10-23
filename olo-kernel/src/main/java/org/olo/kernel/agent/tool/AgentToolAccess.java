/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.olo.toolgate.client.ToolGateClient;

import java.util.Objects;
import java.util.Optional;

/**
 * Worker-wide access to OLO ToolGate. When disabled, agents run without tools exactly as before.
 *
 * @param client               the ToolGate client, or {@code null} when disabled
 * @param defaultMaxToolRounds tool rounds per agent turn when the AGENT node does not set {@code maxToolRounds}
 * @param identityPolicy       which agent identities a run may use; required when enabled
 */
public record AgentToolAccess(ToolGateClient client, int defaultMaxToolRounds, AgentIdentityPolicy identityPolicy,
                              boolean userScoped, boolean viaAgent) {

    private static final AgentToolAccess DISABLED = new AgentToolAccess(null, 0, AgentIdentityPolicy.denyAll(), false, false);

    public AgentToolAccess {
        if (defaultMaxToolRounds < 0) {
            throw new IllegalArgumentException("defaultMaxToolRounds must not be negative");
        }
        Objects.requireNonNull(identityPolicy, "identityPolicy");
    }

    public AgentToolAccess(ToolGateClient client, int defaultMaxToolRounds, AgentIdentityPolicy identityPolicy) {
        this(client, defaultMaxToolRounds, identityPolicy, false, false);
    }

    public static AgentToolAccess disabled() {
        return DISABLED;
    }

    public static AgentToolAccess enabled(ToolGateClient client, int defaultMaxToolRounds, AgentIdentityPolicy identityPolicy) {
        return new AgentToolAccess(Objects.requireNonNull(client, "client"), defaultMaxToolRounds, identityPolicy);
    }

    /**
     * Tools run as the run's verified user (M3.3): a run without one is refused, and agent workload tokens are never used
     * in its place.
     */
    public static AgentToolAccess userScoped(ToolGateClient client, int defaultMaxToolRounds, AgentIdentityPolicy identityPolicy) {
        return new AgentToolAccess(Objects.requireNonNull(client, "client"), defaultMaxToolRounds, identityPolicy, true, false);
    }

    /**
     * Path B (M3.10): tools run with the run's verified user as subject and the mapped agent as actor, so ToolGate needs
     * both the user's grant and the agent's capability. A run without a verified user is refused, as in user-scoped mode.
     */
    public static AgentToolAccess userScopedViaAgent(ToolGateClient client, int defaultMaxToolRounds, AgentIdentityPolicy identityPolicy) {
        return new AgentToolAccess(Objects.requireNonNull(client, "client"), defaultMaxToolRounds, identityPolicy, true, true);
    }

    public boolean isEnabled() {
        return client != null;
    }

    public Optional<ToolGateClient> toolGate() {
        return Optional.ofNullable(client);
    }
}
