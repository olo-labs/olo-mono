/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client.impl;

import org.olo.toolgate.client.AgentNames;
import org.olo.toolgate.client.ToolGateCredentialResolver;
import org.olo.toolgate.client.ToolGateException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Looks up a workload token in {@code OLO_TOOLGATE_TOKEN_<AGENT_NAME>} first, then in
 * {@code <credentialsDir>/<agentName>.token}. Files are read on every call so rotated tokens apply immediately.
 */
public final class FileAndEnvCredentialResolver implements ToolGateCredentialResolver {

    private final Path credentialsDir;
    private final Map<String, String> environment;

    public FileAndEnvCredentialResolver(Path credentialsDir) {
        this(credentialsDir, System.getenv());
    }

    public FileAndEnvCredentialResolver(Path credentialsDir, Map<String, String> environment) {
        this.credentialsDir = credentialsDir;
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public String resolveToken(String agentName) {
        String name;
        try {
            name = AgentNames.requireValid(agentName);
        } catch (IllegalArgumentException e) {
            throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING, e.getMessage(), e);
        }

        String variable = AgentNames.tokenEnvironmentVariable(name);
        String fromEnv = environment.get(variable);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        if (credentialsDir != null) {
            Path file = credentialsDir.resolve(name + ".token");
            if (Files.isRegularFile(file)) {
                try {
                    String token = Files.readString(file, StandardCharsets.UTF_8).trim();
                    if (!token.isEmpty()) {
                        return token;
                    }
                } catch (IOException e) {
                    throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING,
                            "cannot read ToolGate token file for agent '" + name + "': " + file, e);
                }
            }
        }

        throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING,
                "no ToolGate workload token for agent '" + name + "': set " + variable
                        + (credentialsDir != null ? " or create " + credentialsDir.resolve(name + ".token") : ""));
    }
}
