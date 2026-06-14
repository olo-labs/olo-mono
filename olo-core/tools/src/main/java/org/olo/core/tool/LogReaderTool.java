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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OloTool(
        id = CoreToolIds.LOG_READER,
        name = "Log Reader",
        description = "Reads application log lines from a configured folder filtered by time range",
        category = "observability",
        emoji = "📜",
        tags = {"logs", "observability", "incident"},
        examples = {
            "Fetch error logs during an outage window",
            "Correlate gateway timeouts with incident start time"
        },
        arguments = {
            @OloProperty(
                    name = "startTime",
                    label = "Start Time",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Inclusive start of the query window (ISO-8601)",
                    placeholder = "2026-06-14T14:30:00Z",
                    group = "Time Range",
                    order = 0),
            @OloProperty(
                    name = "endTime",
                    label = "End Time",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Inclusive end of the query window (ISO-8601)",
                    placeholder = "2026-06-14T14:31:00Z",
                    group = "Time Range",
                    order = 1)
        },
        configuration = {
            @OloProperty(
                    name = "dataFolder",
                    label = "Log Folder",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Folder containing .log files to scan",
                    placeholder = "demo-data/logs",
                    group = "Data Source",
                    order = 0)
        })
@ToolId(CoreToolIds.LOG_READER)
@ImplementationId(CoreToolIds.LOG_READER)
public final class LogReaderTool implements Tool {

    static final String DEFAULT_DATA_FOLDER = "demo-data/logs";

    @Override
    public String toolId() {
        return CoreToolIds.LOG_READER;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            var folder = ObservabilitySupport.resolveDataFolder(request, "dataFolder", DEFAULT_DATA_FOLDER);
            var range = ObservabilitySupport.parseTimeRange(request);
            List<String> lines = ObservabilitySupport.readLogLines(folder, range);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("dataFolder", folder.toString());
            output.put("startTime", range.start().toString());
            output.put("endTime", range.end().toString());
            output.put("lineCount", lines.size());
            output.put("lines", lines);
            output.put("content", String.join(System.lineSeparator(), lines));
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Log reader failed: " + e.getMessage(), e);
        }
    }
}
