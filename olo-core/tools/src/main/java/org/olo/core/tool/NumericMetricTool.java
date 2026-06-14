package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

@OloTool(
        id = CoreToolIds.NUMERIC_METRIC,
        name = "Numeric Metric",
        description = "Reads generic numeric metrics (latency, error rate, etc.) from CSV files in a configured folder",
        category = "observability",
        emoji = "📈",
        tags = {"metrics", "latency", "observability", "incident"},
        examples = {
            "Inspect p95 latency spike during outage",
            "Compare request latency before and during failure window"
        },
        arguments = {
            @OloProperty(
                    name = "startTime",
                    label = "Start Time",
                    type = OloPropertyType.STRING,
                    required = true,
                    placeholder = "2026-06-14T14:29:00Z",
                    group = "Time Range",
                    order = 0),
            @OloProperty(
                    name = "endTime",
                    label = "End Time",
                    type = OloPropertyType.STRING,
                    required = true,
                    placeholder = "2026-06-14T14:32:00Z",
                    group = "Time Range",
                    order = 1)
        },
        configuration = {
            @OloProperty(
                    name = "dataFolder",
                    label = "Metric Folder",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Folder containing numeric metric CSV files (timestamp,value columns; value = ms)",
                    placeholder = "demo-data/latency",
                    group = "Data Source",
                    order = 0),
            @OloProperty(
                    name = "metricName",
                    label = "Metric Name",
                    type = OloPropertyType.STRING,
                    defaultValue = "latencyMs",
                    description = "Label returned in tool output for the sampled metric series",
                    placeholder = "latencyMs",
                    group = "Data Source",
                    order = 1)
        })
@ToolId(CoreToolIds.NUMERIC_METRIC)
@ImplementationId(CoreToolIds.NUMERIC_METRIC)
public final class NumericMetricTool implements Tool {

    static final String DEFAULT_DATA_FOLDER = "demo-data/latency";

    @Override
    public String toolId() {
        return CoreToolIds.NUMERIC_METRIC;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String metricName = ToolArgs.string(request.arguments(), "metricName",
                ToolArgs.string(request.configuration(), "metricName", "latencyMs"));
        return MetricToolSupport.readMetrics(
                request,
                DEFAULT_DATA_FOLDER,
                "Numeric metric tool failed",
                metricName,
                "peak" + capitalize(metricName),
                "average" + capitalize(metricName));
    }

    private static String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
