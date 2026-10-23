/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rules for agent names used as ToolGate identities.
 */
public final class AgentNames {

    /** Letters, digits, dot, underscore and dash; 1 to 128 characters. Safe as a file name and an env var suffix. */
    public static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private AgentNames() {
    }

    public static boolean isValid(String agentName) {
        return agentName != null && PATTERN.matcher(agentName).matches();
    }

    public static String requireValid(String agentName) {
        if (!isValid(agentName)) {
            throw new IllegalArgumentException("invalid agent name '" + agentName
                    + "': use 1-128 letters, digits, '.', '_' or '-', starting with a letter or digit");
        }
        return agentName;
    }

    /** {@code log-failure.agent} → {@code OLO_TOOLGATE_TOKEN_LOG_FAILURE_AGENT}. */
    public static String tokenEnvironmentVariable(String agentName) {
        return "OLO_TOOLGATE_TOKEN_" + requireValid(agentName).replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
    }
}
