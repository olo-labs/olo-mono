/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
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
        id = CoreToolIds.CPU_USAGE,
        name = "CPU Usage",
        description = "Reads CPU usage metrics from CSV files in a configured folder filtered by time range",
        category = "observability",
        emoji = "🖥️",
        tags = {"cpu", "metrics", "observability", "incident"},
        examples = {
            "Check CPU spike during payment gateway outage",
            "Compare baseline and peak CPU during incident"
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
                    label = "CPU Metrics Folder",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Folder containing CPU metric CSV files (timestamp,value columns; value = cpu %)",
                    placeholder = "demo-data/cpu",
                    group = "Data Source",
                    order = 0)
        })
@ToolId(CoreToolIds.CPU_USAGE)
@ImplementationId(CoreToolIds.CPU_USAGE)
public final class CpuUsageTool implements Tool {

    static final String DEFAULT_DATA_FOLDER = "demo-data/cpu";

    @Override
    public String toolId() {
        return CoreToolIds.CPU_USAGE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return MetricToolSupport.readMetrics(
                request,
                DEFAULT_DATA_FOLDER,
                "CPU usage tool failed",
                "cpuPercent",
                "peakCpuPercent",
                "averageCpuPercent");
    }
}
