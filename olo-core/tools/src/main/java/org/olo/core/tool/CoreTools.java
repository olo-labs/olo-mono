/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.olo.spi.tool.Tool;

import java.util.List;

/**
 * Factory for built-in tool implementations.
 */
public final class CoreTools {

    private CoreTools() {
    }

    public static List<Tool> all() {
        return List.of(
                new HttpTool(),
                new CalculatorTool(),
                new WebSearchTool(),
                new LogReaderTool(),
                new CpuUsageTool(),
                new MemoryUsageTool(),
                new NumericMetricTool(),
                new RecentlyChangedCodeTool(),
                new ResearchLiteratureTool(),
                new TravelDestinationTool(),
                new TravelOffersTool());
    }
}
