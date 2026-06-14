package org.olo.core.tool;

import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MetricToolSupport {

    private MetricToolSupport() {
    }

    static ToolResult readMetrics(
            ToolRequest request,
            String defaultFolder,
            String failurePrefix,
            String samplesKey,
            String peakKey,
            String averageKey) {
        try {
            var folder = ObservabilitySupport.resolveDataFolder(request, "dataFolder", defaultFolder);
            var range = ObservabilitySupport.parseTimeRange(request);
            List<Map<String, Object>> samples = ObservabilitySupport.readMetricRows(folder, range);
            double peak = samples.stream()
                    .mapToDouble(row -> ((Number) row.get("value")).doubleValue())
                    .max()
                    .orElse(0.0);
            double average = samples.stream()
                    .mapToDouble(row -> ((Number) row.get("value")).doubleValue())
                    .average()
                    .orElse(0.0);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("dataFolder", folder.toString());
            output.put("startTime", range.start().toString());
            output.put("endTime", range.end().toString());
            output.put("sampleCount", samples.size());
            output.put(peakKey, peak);
            output.put(averageKey, average);
            output.put(samplesKey, samples);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure(failurePrefix + ": " + e.getMessage(), e);
        }
    }
}
