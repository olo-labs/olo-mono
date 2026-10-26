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

class GraphLogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listLogsReadsMergedGraphMetadata() throws Exception {
        Files.writeString(
                tempDir.resolve("tool-call-agent-agent-2026.json"),
                """
                {
                  "timestamp": "2026-06-27T08:52:04Z",
                  "kind": "tool-call",
                  "workflowId": "agent",
                  "plannerNodeId": "agent",
                  "mergedGraph": {
                    "id": "agent",
                    "label": "Agent (injected)",
                    "nodes": [
                      { "id": "dyn-tool", "type": "TOOL", "label": "CPU Usage" }
                    ]
                  }
                }
                """);

        OloConfigurationProperties properties = new OloConfigurationProperties();
        properties.setLogDirectory(tempDir.toString());
        GraphLogService service = new GraphLogService(properties);

        List<GraphLogService.GraphLogSummary> logs = service.listLogs();

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).fileName()).isEqualTo("tool-call-agent-agent-2026.json");
        assertThat(logs.get(0).kind()).isEqualTo("tool-call");
        assertThat(logs.get(0).workflowId()).isEqualTo("agent");
        assertThat(logs.get(0).label()).isEqualTo("Agent (injected)");
        assertThat(logs.get(0).toolLabels()).containsExactly("CPU Usage");
    }

    @Test
    void readMergedGraphReturnsWorkflowDocument() throws Exception {
        Files.writeString(
                tempDir.resolve("entry.json"),
                """
                {
                  "kind": "dynamic-graph",
                  "mergedGraph": {
                    "id": "demo",
                    "label": "Demo",
                    "nodes": []
                  }
                }
                """);

        OloConfigurationProperties properties = new OloConfigurationProperties();
        properties.setLogDirectory(tempDir.toString());
        GraphLogService service = new GraphLogService(properties);

        assertThat(service.readMergedGraph("entry.json").get("id").asText()).isEqualTo("demo");
    }
}
