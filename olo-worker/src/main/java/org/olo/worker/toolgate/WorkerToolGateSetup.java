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
        org.olo.kernel.knowledge.KnowledgeRuntime.reset();
        if (!settings.toolgateEnabled()) {
            KernelRuntimeHolder.setToolAccess(AgentToolAccess.disabled());
            log.info("ToolGate disabled: LLM agents run without tools");
            return AgentToolAccess.disabled();
        }

        ToolGateSettings config = settings.toolgate();
        if (!config.isUserScoped() || Boolean.FALSE.equals(config.getDelegatedAgents())) {
            throw new IllegalStateException("Model-selected ToolGate tools require userCredentialsUrl and delegated agent authentication");
        }
        Path caCertificate = WorkerSettings.resolveAgainst(config.getCaCertificate(), configurationBaseDirectory);
        Path credentialsDir = WorkerSettings.resolveAgainst(config.getCredentialsDir(), configurationBaseDirectory);
        org.olo.toolgate.client.ToolGateSettings clientSettings = new org.olo.toolgate.client.ToolGateSettings(
                URI.create(config.getMcpUrl()),
                caCertificate,
                credentialsDir,
                Duration.ofMillis(config.resolveTimeoutMs()));
        org.olo.toolgate.client.UserCredentialProvider users = config.isUserScoped() ? userCredentials(config) : null;
        ToolGateClient client = new McpToolGateClient(clientSettings, new FileAndEnvCredentialResolver(credentialsDir), users);

        if (Boolean.getBoolean(SKIP_PROBE_PROPERTY)) {
            log.warn("ToolGate probe skipped ({}=true)", SKIP_PROBE_PROPERTY);
        } else {
            client.probe();
        }

        AgentToolAccess access = AgentToolAccess.userScopedViaAgent(client, config.resolveMaxToolRounds(), identityPolicy(config));
        KernelRuntimeHolder.setToolAccess(access);
        org.olo.kernel.knowledge.KnowledgeRuntime.install(new org.olo.kernel.knowledge.ConversationLifecycle(new org.olo.toolgate.client.GovernedCalls(client)));
        org.olo.kernel.knowledge.KnowledgeRuntime.installDocuments(new org.olo.toolgate.client.GovernedCalls(client));
        log.info("ToolGate enabled: gateway={}, userCredentials={}, credentialsDir={}, caCertificate={}, maxToolRounds={}, agentMappings={}",
                clientSettings.mcpUrl(),
                config.isUserScoped() ? config.getUserCredentialsUrl() : "(agent workload tokens)",
                credentialsDir != null ? credentialsDir : "(env vars only)",
                caCertificate != null ? caCertificate : "(JVM trust store)",
                access.defaultMaxToolRounds(),
                config.getAgentMappings().size());
        if (config.getAgentMappings().isEmpty()) {
            log.warn("ToolGate has no agentMappings: every agent identity is refused until operators map them");
        }
        return access;
    }

    /**
     * User credentials come from the OLO backend, requested with the worker callback key. Without that key the worker
     * refuses to start rather than run user-scoped tools under another identity.
     */
    static org.olo.toolgate.client.UserCredentialProvider userCredentials(ToolGateSettings config) {
        org.olo.kernel.context.callback.CallbackSigner signer = org.olo.kernel.context.callback.CallbackSigner.fromEnvironment()
                .orElseThrow(() -> new IllegalStateException("toolgate.userCredentialsUrl needs "
                        + org.olo.kernel.context.callback.CallbackSigner.ENV_KEY_ID + " and "
                        + org.olo.kernel.context.callback.CallbackSigner.ENV_KEY + " to sign credential requests"));
        return new org.olo.toolgate.client.impl.BackendUserCredentialProvider(URI.create(config.getUserCredentialsUrl()),
                signer::sign, Duration.ofMillis(config.resolveTimeoutMs()));
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
