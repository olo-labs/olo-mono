/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.validation;

import org.junit.jupiter.api.Test;
import org.olo.worker.config.model.AgentMappingSettings;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.ToolGateSettings;

import java.util.List;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerConfigurationValidatorTest {

    @Test
    void acceptsValidConfiguration() {
        WorkerConfiguration configuration = validConfiguration();

        assertThat(WorkerConfigurationValidator.validate(configuration).isValid()).isTrue();
    }

    @Test
    void rejectsMissingPort() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().host("127.0.0.1").build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("/defs")
                        .build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("server.port"));
    }

    @Test
    void rejectsInvalidPort() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(70000).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("/defs")
                        .build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("server.port"));
    }

    @Test
    void rejectsMissingScanFolder() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder().build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("scanFolder"));
    }

    @Test
    void disabledToolGateNeedsNoSettings() {
        WorkerConfiguration configuration = withToolGate(ToolGateSettings.builder().enabled(false).build());

        assertThat(WorkerConfigurationValidator.validate(configuration).isValid()).isTrue();
    }

    @Test
    void enabledToolGateNeedsAnHttpMcpUrl() {
        assertThat(WorkerConfigurationValidator.validate(withToolGate(ToolGateSettings.builder()
                .enabled(true).build())).getErrors())
                .anyMatch(error -> error.contains("toolgate.mcpUrl is required"));
        assertThat(WorkerConfigurationValidator.validate(withToolGate(ToolGateSettings.builder()
                .enabled(true).mcpUrl("ftp://gateway/mcp").build())).getErrors())
                .anyMatch(error -> error.contains("toolgate.mcpUrl must be an http(s) URL"));
        assertThat(WorkerConfigurationValidator.validate(withToolGate(ToolGateSettings.builder()
                .enabled(true).mcpUrl("https://localhost:18451/mcp").build())).isValid()).isTrue();
    }

    @Test
    void rejectsBadToolGateLimits() {
        WorkerConfiguration configuration = withToolGate(ToolGateSettings.builder()
                .enabled(true)
                .mcpUrl("https://localhost:18451/mcp")
                .timeoutMs(0)
                .maxToolRounds(-1)
                .credentialsDir(" ")
                .build());

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("toolgate.timeoutMs"))
                .anyMatch(error -> error.contains("toolgate.maxToolRounds"))
                .anyMatch(error -> error.contains("toolgate.credentialsDir"));
    }

    @Test
    void acceptsExactAgentMappings() {
        WorkerConfiguration configuration = withToolGate(enabledToolGate(List.of(
                mapping("tenant-a", "incident-recovery", List.of("triage-agent", "remediation-agent")),
                mapping("*", "triage-agent", List.of("triage-agent")))));

        assertThat(WorkerConfigurationValidator.validate(configuration).isValid()).isTrue();
    }

    @Test
    void rejectsBadAgentMappings() {
        WorkerConfiguration configuration = withToolGate(enabledToolGate(List.of(
                mapping(" ", "w1", List.of("a")),
                mapping("t", "*", List.of("a")),
                mapping("t", "w2", List.of()),
                mapping("t", "w3", List.of("../admin")),
                mapping("t", "w4", List.of("olo-platform-service")),
                mapping("t", "w4", List.of("b")))));

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(e -> e.contains("agentMappings[0].tenant is required"))
                .anyMatch(e -> e.contains("agentMappings[1].workflow must be an exact workflow id"))
                .anyMatch(e -> e.contains("agentMappings[2].agents must list at least one"))
                .anyMatch(e -> e.contains("agentMappings[3].agents contains an invalid agent name"))
                .anyMatch(e -> e.contains("agentMappings[4].agents may not contain the reserved principal"))
                .anyMatch(e -> e.contains("agentMappings[5] repeats an earlier tenant and workflow"));
    }

    @Test
    void customReservedPrincipalsAreEnforced() {
        WorkerConfiguration configuration = withToolGate(ToolGateSettings.builder()
                .enabled(true)
                .mcpUrl("https://localhost:18451/mcp")
                .reservedPrincipals(List.of("ops-indexer"))
                .agentMappings(List.of(mapping("t", "w", List.of("ops-indexer"))))
                .build());

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(e -> e.contains("reserved principal ops-indexer"));
    }

    private static ToolGateSettings enabledToolGate(List<AgentMappingSettings> mappings) {
        return ToolGateSettings.builder().enabled(true).mcpUrl("https://localhost:18451/mcp").agentMappings(mappings).build();
    }

    private static AgentMappingSettings mapping(String tenant, String workflow, List<String> agents) {
        return AgentMappingSettings.builder().tenant(tenant).workflow(workflow).agents(agents).build();
    }

    @Test
    void toolGateDefaults() {
        ToolGateSettings settings = ToolGateSettings.builder().enabled(true).build();

        assertThat(settings.resolveTimeoutMs()).isEqualTo(ToolGateSettings.DEFAULT_TIMEOUT_MS);
        assertThat(settings.resolveMaxToolRounds()).isEqualTo(ToolGateSettings.DEFAULT_MAX_TOOL_ROUNDS);
    }

    private static WorkerConfiguration withToolGate(ToolGateSettings toolgate) {
        return WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder().scanFolder("/defs").build())
                .toolgate(toolgate)
                .build();
    }

    private static WorkerConfiguration validConfiguration() {
        return WorkerConfiguration.builder()
                .id("worker")
                .name("Worker")
                .server(ServerSettings.builder().host("0.0.0.0").port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("../olo-definition/olo-configuration/default")
                        .recursive(false)
                        .build())
                .build();
    }
}
