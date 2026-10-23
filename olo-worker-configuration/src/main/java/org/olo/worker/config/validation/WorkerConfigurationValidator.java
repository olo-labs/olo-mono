/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.validation;

import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.AgentMappingSettings;
import org.olo.worker.config.model.CacheSettings;
import org.olo.worker.config.model.InputSettings;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.ToolGateSettings;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Structural validation for a loaded {@link WorkerConfiguration}.
 */
public final class WorkerConfigurationValidator {

    /** Same rule as {@code org.olo.toolgate.client.AgentNames}; this module doesn't depend on the client. */
    private static final Pattern AGENT_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private WorkerConfigurationValidator() {
    }

    public static WorkerConfigurationValidationResult validate(WorkerConfiguration configuration) {
        List<String> errors = new ArrayList<>();
        if (configuration == null) {
            return WorkerConfigurationValidationResult.failure(List.of("configuration must not be null"));
        }
        if (isBlank(configuration.getId())) {
            errors.add("worker id is required");
        }
        validateServer(configuration.getServer(), errors);
        validateWorkflowDefinitions(configuration.getWorkflowDefinitions(), errors);
        validateInput(configuration.getInput(), errors);
        validateCache(configuration.getCache(), errors);
        validateToolGate(configuration.getToolgate(), errors);
        return errors.isEmpty()
                ? WorkerConfigurationValidationResult.success()
                : WorkerConfigurationValidationResult.failure(errors);
    }

    public static void validateOrThrow(WorkerConfiguration configuration) {
        WorkerConfigurationValidationResult result = validate(configuration);
        if (!result.isValid()) {
            throw new WorkerConfigurationException(String.join("; ", result.getErrors()));
        }
    }

    private static void validateServer(ServerSettings server, List<String> errors) {
        if (server == null) {
            errors.add("server settings are required");
            return;
        }
        if (server.getPort() == null) {
            errors.add("server.port is required");
        } else if (server.getPort() < 1 || server.getPort() > 65535) {
            errors.add("server.port must be between 1 and 65535");
        }
        if (server.getHost() != null && server.getHost().isBlank()) {
            errors.add("server.host must not be blank when set");
        }
    }

    private static void validateWorkflowDefinitions(WorkflowDefinitionsSettings settings, List<String> errors) {
        if (settings == null) {
            errors.add("workflowDefinitions settings are required");
            return;
        }
        if (isBlank(settings.getScanFolder())) {
            errors.add("workflowDefinitions.scanFolder is required");
        }
    }

    private static void validateInput(InputSettings input, List<String> errors) {
        if (input == null || input.getMaxLocalMessageSize() == null) {
            return;
        }
        if (input.getMaxLocalMessageSize() < 1) {
            errors.add("input.maxLocalMessageSize must be at least 1");
        }
    }

    private static void validateCache(CacheSettings cache, List<String> errors) {
        if (cache == null || !cache.isEnabled()) {
            return;
        }
        if (isBlank(cache.getHost())) {
            errors.add("cache.host is required when cache.enabled is true");
        }
        if (cache.getPort() != null && (cache.getPort() < 1 || cache.getPort() > 65535)) {
            errors.add("cache.port must be between 1 and 65535");
        }
    }

    private static void validateToolGate(ToolGateSettings toolgate, List<String> errors) {
        if (toolgate == null || !toolgate.isEnabled()) {
            return;
        }
        if (isBlank(toolgate.getMcpUrl())) {
            errors.add("toolgate.mcpUrl is required when toolgate.enabled is true");
        } else {
            try {
                String scheme = URI.create(toolgate.getMcpUrl()).getScheme();
                if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                    errors.add("toolgate.mcpUrl must be an http(s) URL");
                }
            } catch (IllegalArgumentException e) {
                errors.add("toolgate.mcpUrl is not a valid URL");
            }
        }
        if (toolgate.getTimeoutMs() != null && toolgate.getTimeoutMs() < 1) {
            errors.add("toolgate.timeoutMs must be at least 1");
        }
        if (toolgate.getMaxToolRounds() != null && toolgate.getMaxToolRounds() < 0) {
            errors.add("toolgate.maxToolRounds must not be negative");
        }
        if (toolgate.getCaCertificate() != null && toolgate.getCaCertificate().isBlank()) {
            errors.add("toolgate.caCertificate must not be blank when set");
        }
        if (toolgate.getCredentialsDir() != null && toolgate.getCredentialsDir().isBlank()) {
            errors.add("toolgate.credentialsDir must not be blank when set");
        }
        validateAgentMappings(toolgate, errors);
    }

    private static void validateAgentMappings(ToolGateSettings toolgate, List<String> errors) {
        Set<String> reserved = new HashSet<>();
        for (String principal : toolgate.resolveReservedPrincipals()) {
            if (isBlank(principal)) {
                errors.add("toolgate.reservedPrincipals must not contain blank names");
            } else {
                reserved.add(principal.trim().toLowerCase(Locale.ROOT));
            }
        }
        Set<String> scopes = new HashSet<>();
        for (int i = 0; i < toolgate.getAgentMappings().size(); i++) {
            AgentMappingSettings mapping = toolgate.getAgentMappings().get(i);
            String where = "toolgate.agentMappings[" + i + "]";
            if (mapping == null) {
                errors.add(where + " must not be null");
                continue;
            }
            if (isBlank(mapping.getTenant())) {
                errors.add(where + ".tenant is required (use \"*\" only for single-tenant installations)");
            }
            if (isBlank(mapping.getWorkflow()) || mapping.getWorkflow().contains("*")) {
                errors.add(where + ".workflow must be an exact workflow id");
            }
            if (!scopes.add(mapping.getTenant() + "\u0000" + mapping.getWorkflow())) {
                errors.add(where + " repeats an earlier tenant and workflow; list each pair once");
            }
            if (mapping.getAgents().isEmpty()) {
                errors.add(where + ".agents must list at least one agent name");
            }
            for (String agent : mapping.getAgents()) {
                if (agent == null || !AGENT_NAME.matcher(agent).matches()) {
                    errors.add(where + ".agents contains an invalid agent name: " + agent);
                } else if (reserved.contains(agent.toLowerCase(Locale.ROOT))) {
                    errors.add(where + ".agents may not contain the reserved principal " + agent);
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
