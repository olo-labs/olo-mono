/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.toolgate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.agent.tool.AgentIdentityDeniedException;
import org.olo.kernel.agent.tool.AgentToolAccess;
import org.olo.toolgate.client.ToolGateException;
import org.olo.worker.config.WorkerSettings;
import org.olo.worker.config.model.AgentMappingSettings;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.ToolGateSettings;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerToolGateSetupTest {

    @org.junit.jupiter.api.BeforeEach
    void configureWorkerIdentity() {
        System.setProperty(WorkerToolGateSetup.SKIP_PROBE_PROPERTY, "true");
        System.setProperty("OLO_CALLBACK_KEY_ID", "test-worker");
        System.setProperty("OLO_CALLBACK_KEY", "test-worker-secret-at-least-32-bytes-long");
    }

    @AfterEach
    void reset() {
        KernelRuntimeHolder.setToolAccess(AgentToolAccess.disabled());
        System.clearProperty("OLO_CALLBACK_KEY_ID");
        System.clearProperty("OLO_CALLBACK_KEY");
        System.setProperty(WorkerToolGateSetup.SKIP_PROBE_PROPERTY, "true");
    }

    @Test
    void disabledLeavesAgentsWithoutTools() {
        AgentToolAccess access = WorkerToolGateSetup.install(settings(ToolGateSettings.builder().enabled(false).build()), Path.of("."));

        assertThat(access.isEnabled()).isFalse();
        assertThat(KernelRuntimeHolder.toolAccess().isEnabled()).isFalse();
    }

    @Test
    void enabledInstallsAccessWithTheWorkerDefaultRounds() {
        AgentToolAccess access = WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true)
                .userCredentialsUrl("https://backend.example")
                .mcpUrl("https://localhost:18451/mcp")
                .credentialsDir("toolgate/credentials")
                .maxToolRounds(5)
                .build()), Path.of("config"));

        assertThat(access.isEnabled()).isTrue();
        assertThat(access.defaultMaxToolRounds()).isEqualTo(5);
        assertThat(access.userScoped()).isTrue();
        assertThat(access.viaAgent()).isTrue();
        assertThat(KernelRuntimeHolder.toolAccess()).isSameAs(access);
    }

    @Test
    void unreachableGatewayStopsTheWorker() {
        System.setProperty(WorkerToolGateSetup.SKIP_PROBE_PROPERTY, "false");

        assertThatThrownBy(() -> WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true)
                .userCredentialsUrl("https://backend.example")
                .mcpUrl("http://127.0.0.1:1/mcp")
                .timeoutMs(2000)
                .build()), Path.of(".")))
                .isInstanceOf(ToolGateException.class);
        assertThat(KernelRuntimeHolder.toolAccess().isEnabled()).isFalse();
    }

    @Test
    void agentMappingsBecomeTheIdentityPolicy() {
        AgentToolAccess access = WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true)
                .userCredentialsUrl("https://backend.example")
                .mcpUrl("https://localhost:18451/mcp")
                .agentMappings(List.of(AgentMappingSettings.builder()
                        .tenant("*").workflow("triage-agent").agents(List.of("triage-agent")).build()))
                .build()), Path.of("."));

        assertThatCode(() -> access.identityPolicy().authorize("t1", "triage-agent", "triage-agent"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> access.identityPolicy().authorize("t1", "triage-agent", "payments-admin"))
                .isInstanceOf(AgentIdentityDeniedException.class);
        assertThatThrownBy(() -> access.identityPolicy().authorize("t1", "triage-agent", "olo-platform-service"))
                .isInstanceOf(AgentIdentityDeniedException.class);
    }

    @Test
    void noMappingsRefusesEveryIdentity() {
        AgentToolAccess access = WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true).userCredentialsUrl("https://backend.example").mcpUrl("https://localhost:18451/mcp").build()), Path.of("."));

        assertThatThrownBy(() -> access.identityPolicy().authorize("t1", "triage-agent", "triage-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class);
    }

    @Test
    void relativePathsResolveAgainstTheConfigFolder() {
        assertThat(WorkerSettings.resolveAgainst("toolgate/ca.crt", Path.of("/etc/olo")))
                .isEqualTo(Path.of("/etc/olo/toolgate/ca.crt").normalize());
        assertThat(WorkerSettings.resolveAgainst(null, Path.of("/etc/olo"))).isNull();
    }

    private static WorkerSettings settings(ToolGateSettings toolgate) {
        return new WorkerSettings(WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder().scanFolder("/defs").build())
                .toolgate(toolgate)
                .build());
    }

    @Test
    void modelSelectedToolsCannotDisableTheUserOrAgentAuthorizationChecks() {
        assertThatThrownBy(() -> WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true).mcpUrl("https://gateway.example/mcp").build()), Path.of(".")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("delegated agent authentication");
        assertThatThrownBy(() -> WorkerToolGateSetup.install(settings(ToolGateSettings.builder()
                .enabled(true).userCredentialsUrl("https://backend.example").delegatedAgents(false)
                .mcpUrl("https://gateway.example/mcp").build()), Path.of(".")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("delegated agent authentication");
    }
}
