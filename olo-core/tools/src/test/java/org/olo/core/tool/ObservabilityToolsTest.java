package org.olo.core.tool;

import org.junit.jupiter.api.Test;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolStatus;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityToolsTest {

    private static final String INCIDENT_START = "2026-06-14T14:30:00Z";
    private static final String INCIDENT_END = "2026-06-14T14:31:00Z";
    private static final String METRIC_START = "2026-06-14T14:29:00Z";
    private static final String METRIC_END = "2026-06-14T14:32:00Z";

    private static final ExecutionContext CONTEXT = new ExecutionContext() {
        @Override
        public String getWorkflowId() {
            return "wf-demo";
        }

        @Override
        public String getRunId() {
            return "run-demo";
        }

        @Override
        public String getQueue() {
            return "default";
        }

        @Override
        public Optional<String> getNodeId() {
            return Optional.of("tool-node");
        }

        @Override
        public Optional<String> getCorrelationId() {
            return Optional.of("corr-demo");
        }

        @Override
        public boolean hasVariable(String name) {
            return false;
        }

        @Override
        public Object getVariable(String name) {
            return null;
        }

        @Override
        public void setVariable(String name, Object value) {
            // no-op
        }

        @Override
        public Map<String, Object> getVariables() {
            return Collections.emptyMap();
        }
    };

    @Test
    void logReaderReturnsErrorLinesDuringIncident() {
        LogReaderTool tool = new LogReaderTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.LOG_READER,
                null,
                Map.of("startTime", INCIDENT_START, "endTime", INCIDENT_END),
                Map.of("dataFolder", demoPath("logs").toString()));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("lineCount", 6);
        assertThat(String.valueOf(result.output().get("content"))).contains("ERROR");
        assertThat(String.valueOf(result.output().get("content"))).contains("ConnectionTimeout");
    }

    @Test
    void cpuUsageShowsSpikeDuringIncident() {
        CpuUsageTool tool = new CpuUsageTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.CPU_USAGE,
                null,
                Map.of("startTime", METRIC_START, "endTime", METRIC_END),
                Map.of("dataFolder", demoPath("cpu").toString()));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("peakCpuPercent", 97.4);
        assertThat(result.output()).containsKey("cpuPercent");
    }

    @Test
    void memoryUsageShowsSpikeDuringIncident() {
        MemoryUsageTool tool = new MemoryUsageTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.MEMORY_USAGE,
                null,
                Map.of("startTime", METRIC_START, "endTime", METRIC_END),
                Map.of("dataFolder", demoPath("memory").toString()));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("peakMemoryMb", 1536.0);
        assertThat(result.output()).containsKey("memoryMb");
    }

    @Test
    void numericMetricShowsLatencySpikeDuringIncident() {
        NumericMetricTool tool = new NumericMetricTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.NUMERIC_METRIC,
                null,
                Map.of("startTime", METRIC_START, "endTime", METRIC_END),
                Map.of(
                        "dataFolder", demoPath("latency").toString(),
                        "metricName", "latencyMs"));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("peakLatencyMs", 3120.0);
        assertThat(result.output()).containsKey("latencyMs");
    }

    @Test
    void recentlyChangedCodeReturnsPatchFilesFromFolder() {
        RecentlyChangedCodeTool tool = new RecentlyChangedCodeTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.RECENTLY_CHANGED_CODE,
                null,
                Map.of("limit", 5),
                Map.of("dataFolder", demoPath("recent-changes").toString()));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("changeCount", 3);
        assertThat(String.valueOf(result.output().get("content"))).contains("Pull-Request: #842");
        assertThat(String.valueOf(result.output().get("content"))).contains("CONNECT_TIMEOUT");
    }

    @Test
    void recentlyChangedCodeFiltersByPullRequestNumber() {
        RecentlyChangedCodeTool tool = new RecentlyChangedCodeTool();
        ToolRequest request = new ToolRequest(
                CoreToolIds.RECENTLY_CHANGED_CODE,
                null,
                Map.of("limit", 5, "pullRequestNumber", "839"),
                Map.of("dataFolder", demoPath("recent-changes").toString()));

        var result = tool.invoke(request, CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("changeCount", 1);
        assertThat(String.valueOf(result.output().get("content"))).contains("839-retry-pool-resize.patch");
        assertThat(String.valueOf(result.output().get("content"))).contains("RETRY_POOL_SIZE = 128");
    }

    private static Path demoPath(String folder) {
        return Path.of("demo-data", folder).toAbsolutePath().normalize();
    }
}
