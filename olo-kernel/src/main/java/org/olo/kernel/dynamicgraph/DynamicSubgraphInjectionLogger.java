/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Writes an audit log file under {@code olo-configuration/log/} whenever a dynamic subgraph is merged
 * into the active workflow graph.
 */
public final class DynamicSubgraphInjectionLogger {

    private static final Logger log = LoggerFactory.getLogger(DynamicSubgraphInjectionLogger.class);
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final JsonWorkflowSerializer WORKFLOW_JSON = new JsonWorkflowSerializer();

    private DynamicSubgraphInjectionLogger() {
    }

    public static void logInjection(InjectionRecord record) {
        Objects.requireNonNull(record, "record");
        try {
            Path logDirectory = DynamicSubgraphInjectionLogPaths.resolveLogDirectory();
            Files.createDirectories(logDirectory);
            Path target = logDirectory.resolve(buildFileName(record));
            Files.writeString(target, buildPayload(record));
            log.info(
                    "Dynamic subgraph injection logged: kind={}, workflowId={}, file={}",
                    record.kind(),
                    record.workflowId(),
                    target);
        } catch (Exception e) {
            log.warn(
                    "Failed to write dynamic subgraph injection log for workflow {}: {}",
                    record.workflowId(),
                    e.getMessage());
        }
    }

    static String buildFileName(InjectionRecord record) {
        String workflow = sanitize(record.workflowId());
        String planner = sanitize(record.plannerNodeId());
        return record.kind().wireName()
                + "-"
                + workflow
                + "-"
                + planner
                + "-"
                + Instant.now().toString().replace(":", "-")
                + "-"
                + System.nanoTime()
                + ".json";
    }

    static String buildPayload(InjectionRecord record) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("timestamp", Instant.now().toString());
        root.put("kind", record.kind().wireName());
        root.put("workflowId", record.workflowId());
        root.put("plannerNodeId", record.plannerNodeId());
        root.put("continueNodeId", record.continueNodeId());
        root.put("entryNodeId", record.entryNodeId());
        root.put("strategy", record.strategyName());
        if (record.sourceJson() != null) {
            root.put("sourceJson", record.sourceJson());
        }
        root.set("mergedGraph", MAPPER.readTree(WORKFLOW_JSON.serialize(record.mergedGraph())));
        return MAPPER.writeValueAsString(root);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-|-$", "");
    }

    public record InjectionRecord(
            Kind kind,
            String strategyName,
            String workflowId,
            String plannerNodeId,
            String continueNodeId,
            String entryNodeId,
            String sourceJson,
            WorkflowDefinition mergedGraph) {

        public enum Kind {
            DYNAMIC_GRAPH("dynamic-graph"),
            TOOL_CALL("tool-call");

            private final String wireName;

            Kind(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }
        }
    }
}
