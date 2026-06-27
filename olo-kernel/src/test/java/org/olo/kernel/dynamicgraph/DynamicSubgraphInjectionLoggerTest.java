package org.olo.kernel.dynamicgraph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSubgraphInjectionLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesInjectionLogFileToConfiguredDirectory() throws Exception {
        System.setProperty(DynamicSubgraphInjectionLogPaths.LOG_DIR_PROPERTY, tempDir.toString());
        try {
            WorkflowDefinition merged = WorkflowBuilder.create("Merged")
                    .id("demo")
                    .queue("demo")
                    .startNode("start")
                    .endNode("end")
                    .connect("start", "end")
                    .build();

            DynamicSubgraphInjectionLogger.logInjection(new DynamicSubgraphInjectionLogger.InjectionRecord(
                    DynamicSubgraphInjectionLogger.InjectionRecord.Kind.DYNAMIC_GRAPH,
                    "dynamic-graph-expansion",
                    "dynamic-graph-creation",
                    "graph-planner",
                    "end",
                    "dyn-step-1",
                    "{\"id\":\"subgraph\"}",
                    merged));

            try (var files = Files.list(tempDir)) {
                Path logFile = files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .findFirst()
                        .orElseThrow();
                String content = Files.readString(logFile);
                assertThat(logFile.getFileName().toString()).startsWith("dynamic-graph-dynamic-graph-creation-graph-planner-");
                assertThat(content).contains("\"kind\" : \"dynamic-graph\"");
                assertThat(content).contains("\"entryNodeId\" : \"dyn-step-1\"");
                assertThat(content).contains("\"mergedGraph\"");
            }
        } finally {
            System.clearProperty(DynamicSubgraphInjectionLogPaths.LOG_DIR_PROPERTY);
        }
    }

    @Test
    void writesOnlyToLogDirectory() throws Exception {
        Path currentActive = Files.createTempDirectory("current-active");
        System.setProperty(DynamicSubgraphInjectionLogPaths.LOG_DIR_PROPERTY, tempDir.toString());
        System.setProperty(DynamicSubgraphInjectionLogPaths.CURRENT_ACTIVE_DIR_PROPERTY, currentActive.toString());
        try {
            WorkflowDefinition merged = WorkflowBuilder.create("Agent")
                    .id("agent")
                    .queue("agent")
                    .startNode("start")
                    .endNode("end")
                    .connect("start", "end")
                    .build();

            DynamicSubgraphInjectionLogger.logInjection(new DynamicSubgraphInjectionLogger.InjectionRecord(
                    DynamicSubgraphInjectionLogger.InjectionRecord.Kind.TOOL_CALL,
                    "tool-call-expansion",
                    "agent",
                    "agent",
                    "end",
                    "tool-dyn-step-0",
                    "{\"toolCalls\":[{\"toolId\":\"olo-core:cpu-usage\"}],\"directResponse\":null}",
                    merged));

            try (var logFiles = Files.list(tempDir)) {
                assertThat(logFiles.filter(path -> path.getFileName().toString().endsWith(".json")).count())
                        .isEqualTo(1);
            }
            try (var activeFiles = Files.list(currentActive)) {
                assertThat(activeFiles.findAny()).isEmpty();
            }
        } finally {
            System.clearProperty(DynamicSubgraphInjectionLogPaths.LOG_DIR_PROPERTY);
            System.clearProperty(DynamicSubgraphInjectionLogPaths.CURRENT_ACTIVE_DIR_PROPERTY);
        }
    }
}
