/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * Connection settings for an OLO ToolGate Gateway.
 *
 * @param mcpUrl         Gateway MCP endpoint, for example {@code https://gateway.example.com/mcp}
 * @param caCertificate  PEM file with the CA that signs the Gateway certificate; {@code null} uses the JVM trust store
 * @param credentialsDir folder holding {@code <agentName>.token} workload tokens; {@code null} means env vars only
 * @param timeout        per-request timeout, covering the whole governed tool call
 */
public record ToolGateSettings(URI mcpUrl, Path caCertificate, Path credentialsDir, Duration timeout) {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    public ToolGateSettings {
        Objects.requireNonNull(mcpUrl, "mcpUrl");
        String scheme = mcpUrl.getScheme() == null ? "" : mcpUrl.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("ToolGate mcpUrl must be http(s): " + mcpUrl);
        }
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("ToolGate timeout must be positive: " + timeout);
        }
    }
}
