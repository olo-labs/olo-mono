/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.validation;

import org.junit.jupiter.api.Test;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.ToolGateSettings;
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
