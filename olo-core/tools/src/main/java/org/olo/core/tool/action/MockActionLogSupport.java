/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.core.tool.observability.ObservabilitySupport;
import org.olo.spi.context.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Appends mock human-approved action entries to a JSONL execution log for demo audit trails. */
public final class MockActionLogSupport {

    public static final String DEFAULT_LOG_FOLDER = "demo-data/mock-actions";
    public static final String LOG_FILE_NAME = "execution.log";

    private static final ObjectMapper JSON = new ObjectMapper();

    private MockActionLogSupport() {
    }

    public static Map<String, Object> recordMockExecution(
            ExecutionContext context, String toolId, String action, Map<String, Object> arguments)
            throws IOException {
        String confirmationId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("confirmationId", confirmationId);
        logEntry.put("timestamp", timestamp.toString());
        logEntry.put("toolId", toolId);
        logEntry.put("action", action);
        logEntry.put("arguments", arguments == null ? Map.of() : Map.copyOf(arguments));
        logEntry.put("workflowId", context.getWorkflowId());
        logEntry.put("runId", context.getRunId());
        logEntry.put("queue", context.getQueue());
        context.getNodeId().ifPresent(nodeId -> logEntry.put("nodeId", nodeId));
        context.getCorrelationId().ifPresent(correlationId -> logEntry.put("correlationId", correlationId));
        logEntry.put("status", "MOCK_EXECUTED");
        logEntry.put(
                "message",
                "Mock action executed and logged — use confirmationId to verify in the workflow audit trail");

        Path logFile = resolveLogFile();
        Files.createDirectories(logFile.getParent());
        String line = JSON.writeValueAsString(logEntry);
        Files.writeString(
                logFile,
                line + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        Map<String, Object> output = new LinkedHashMap<>();
        if (arguments != null) {
            output.putAll(arguments);
        }
        output.put("confirmationId", confirmationId);
        output.put("logPath", logFile.toAbsolutePath().normalize().toString());
        output.put("logEntry", logEntry);
        output.put("status", "MOCK_EXECUTED");
        output.put("executedAt", timestamp.toString());
        return output;
    }

    static Path resolveLogFile() {
        Path toolsDirectory = ObservabilitySupport.discoverToolsDirectory();
        Path folder;
        if (toolsDirectory != null) {
            folder = toolsDirectory.resolve(DEFAULT_LOG_FOLDER).normalize();
        } else {
            folder = Path.of(DEFAULT_LOG_FOLDER).normalize();
        }
        return folder.resolve(LOG_FILE_NAME);
    }
}
