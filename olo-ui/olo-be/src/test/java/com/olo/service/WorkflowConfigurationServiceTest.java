/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.config.OloConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowConfigurationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listWorkflowsScansNestedSubdirectories() throws Exception {
        Path nested = tempDir.resolve("agents");
        Files.createDirectories(nested);
        Files.writeString(
                nested.resolve("agent.json"),
                """
                {
                  "id": "agent",
                  "label": "Agent",
                  "queue": "oloQueue2"
                }
                """);
        Files.writeString(
                tempDir.resolve("architect.json"),
                """
                {
                  "id": "architect",
                  "label": "Architect",
                  "queue": "oloQueue1"
                }
                """);

        OloConfigurationProperties properties = new OloConfigurationProperties();
        properties.setDirectory(tempDir.toString());
        WorkflowConfigurationService service = new WorkflowConfigurationService(properties);

        List<WorkflowConfigurationService.WorkflowSummary> workflows = service.listWorkflows();

        assertThat(workflows).hasSize(2);
        assertThat(workflows)
                .extracting(WorkflowConfigurationService.WorkflowSummary::fileName)
                .containsExactlyInAnyOrder("agents/agent.json", "architect.json");
    }

    @Test
    void readWorkflowResolvesNestedRelativePath() throws Exception {
        Path nested = tempDir.resolve("agents");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("agent.json"), "{\"id\":\"agent\",\"label\":\"Agent\"}");

        OloConfigurationProperties properties = new OloConfigurationProperties();
        properties.setDirectory(tempDir.toString());
        WorkflowConfigurationService service = new WorkflowConfigurationService(properties);

        assertThat(service.readWorkflow("agents/agent.json").get("id").asText()).isEqualTo("agent");
    }

    @Test
    void listWorkflowsSkipsGraphLogFiles() throws Exception {
        Files.writeString(
                tempDir.resolve("agent.json"),
                """
                {
                  "id": "agent",
                  "label": "Agent"
                }
                """);
        Path logDir = tempDir.resolve("log");
        Files.createDirectories(logDir);
        Files.writeString(
                logDir.resolve("tool-call-agent-agent.json"),
                """
                {
                  "id": "tool-call-agent-agent",
                  "kind": "tool-call"
                }
                """);

        OloConfigurationProperties properties = new OloConfigurationProperties();
        properties.setDirectory(tempDir.toString());
        WorkflowConfigurationService service = new WorkflowConfigurationService(properties);

        List<WorkflowConfigurationService.WorkflowSummary> workflows = service.listWorkflows();

        assertThat(workflows).hasSize(1);
        assertThat(workflows.get(0).fileName()).isEqualTo("agent.json");
    }
}
