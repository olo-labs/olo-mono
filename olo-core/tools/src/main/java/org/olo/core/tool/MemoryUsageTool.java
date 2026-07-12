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

@OloTool(
        id = CoreToolIds.MEMORY_USAGE,
        name = "Memory Usage",
        description = "Reads memory usage metrics from CSV files in a configured folder filtered by time range",
        category = "observability",
        emoji = "💾",
        tags = {"memory", "metrics", "observability", "incident"},
        examples = {
            "Detect memory pressure during gateway timeout incident",
            "Track heap growth leading up to emergency GC"
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
                    label = "Memory Metrics Folder",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Folder containing memory metric CSV files (timestamp,value columns; value = MB)",
                    placeholder = "demo-data/memory",
                    group = "Data Source",
                    order = 0)
        })
@ToolId(CoreToolIds.MEMORY_USAGE)
@ImplementationId(CoreToolIds.MEMORY_USAGE)
public final class MemoryUsageTool implements Tool {

    static final String DEFAULT_DATA_FOLDER = "demo-data/memory";

    @Override
    public String toolId() {
        return CoreToolIds.MEMORY_USAGE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return MetricToolSupport.readMetrics(
                request,
                DEFAULT_DATA_FOLDER,
                "Memory usage tool failed",
                "memoryMb",
                "peakMemoryMb",
                "averageMemoryMb");
    }
}
