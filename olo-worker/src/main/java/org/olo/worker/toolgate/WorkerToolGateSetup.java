/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.toolgate;

import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.agent.tool.AgentIdentityPolicy;
import org.olo.kernel.agent.tool.AgentToolAccess;
import org.olo.kernel.agent.tool.MappedAgentIdentityPolicy;
import org.olo.toolgate.client.ToolGateClient;
import org.olo.toolgate.client.ToolGateException;
import org.olo.toolgate.client.impl.FileAndEnvCredentialResolver;
import org.olo.toolgate.client.impl.McpToolGateClient;
import org.olo.worker.config.WorkerSettings;
import org.olo.worker.config.model.ToolGateSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Installs worker-wide OLO ToolGate access into the kernel from the {@code toolgate} config section.
 */
public final class WorkerToolGateSetup {

    /** System property that skips the Gateway reachability probe (tests). */
    public static final String SKIP_PROBE_PROPERTY = "olo.worker.skipToolGateProbe";

    private static final Logger log = LoggerFactory.getLogger(WorkerToolGateSetup.class);

    private WorkerToolGateSetup() {
    }

    /**
     * @throws ToolGateException when ToolGate is enabled but the Gateway does not answer (fail closed)
     */
    public static AgentToolAccess install(WorkerSettings settings, Path configurationBaseDirectory) {
        if (!settings.toolgateEnabled()) {
            KernelRuntimeHolder.setToolAccess(AgentToolAccess.disabled());
            log.info("ToolGate disabled: LLM agents run without tools");
            return AgentToolAccess.disabled();
        }

        ToolGateSettings config = settings.toolgate();
        Path caCertificate = WorkerSettings.resolveAgainst(config.getCaCertificate(), configurationBaseDirectory);
        Path credentialsDir = WorkerSettings.resolveAgainst(config.getCredentialsDir(), configurationBaseDirectory);
        org.olo.toolgate.client.ToolGateSettings clientSettings = new org.olo.toolgate.client.ToolGateSettings(
                URI.create(config.getMcpUrl()),
                caCertificate,
                credentialsDir,
                Duration.ofMillis(config.resolveTimeoutMs()));
        ToolGateClient client = new McpToolGateClient(clientSettings, new FileAndEnvCredentialResolver(credentialsDir));

        if (Boolean.getBoolean(SKIP_PROBE_PROPERTY)) {
            log.warn("ToolGate probe skipped ({}=true)", SKIP_PROBE_PROPERTY);
        } else {
            client.probe();
        }

        AgentToolAccess access = AgentToolAccess.enabled(client, config.resolveMaxToolRounds(), identityPolicy(config));
        KernelRuntimeHolder.setToolAccess(access);
        log.info("ToolGate enabled: gateway={}, credentialsDir={}, caCertificate={}, maxToolRounds={}, agentMappings={}",
                clientSettings.mcpUrl(),
                credentialsDir != null ? credentialsDir : "(env vars only)",
                caCertificate != null ? caCertificate : "(JVM trust store)",
                access.defaultMaxToolRounds(),
                config.getAgentMappings().size());
        if (config.getAgentMappings().isEmpty()) {
            log.warn("ToolGate has no agentMappings: every agent identity is refused until operators map them");
        }
        return access;
    }

    /** Operator-controlled (tenant, workflow) → agents mappings. Workflow definitions can't change them. */
    static AgentIdentityPolicy identityPolicy(ToolGateSettings config) {
        if (config.getAgentMappings().isEmpty()) {
            return AgentIdentityPolicy.denyAll();
        }
        List<MappedAgentIdentityPolicy.Mapping> mappings = config.getAgentMappings().stream()
                .map(m -> new MappedAgentIdentityPolicy.Mapping(m.getTenant(), m.getWorkflow(), m.getAgents()))
                .toList();
        return new MappedAgentIdentityPolicy(mappings, config.resolveReservedPrincipals());
    }
}
