/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.olo.core.tool.catalog.ScenarioCatalogSupport;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloStability;
import org.olo.annotation.OloExecutionModel;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.List;
import java.util.Map;

@OloTool(
        id = CoreToolIds.RESEARCH_LITERATURE,
        name = "Research literature",
        description = "Returns mock academic paper summaries for research planner scenarios",
        stability = OloStability.EXPERIMENTAL,
        category = "research",
        emoji = "📚",
        tags = {"research", "literature", "scenario"},
        examples = {
            "Find papers on renewable energy storage",
            "Summarize AI safety literature for a briefing"
        },
        arguments = {
            @OloProperty(name = "query", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "topic", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "limit", type = OloPropertyType.NUMBER, required = false, defaultValue = "3")
        },
        executionModel = OloExecutionModel.ACTIVITY)
@ToolId(CoreToolIds.RESEARCH_LITERATURE)
@ImplementationId(CoreToolIds.RESEARCH_LITERATURE)
public final class ResearchLiteratureTool implements Tool {

    static final String DEMO_FOLDER = "demo-data/research/literature";

    @Override
    public String toolId() {
        return CoreToolIds.RESEARCH_LITERATURE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            List<Map<String, Object>> papers = ScenarioCatalogSupport.readCatalog(
                    DEMO_FOLDER, request, "query", "topic");
            return ToolResult.success(
                    "Research literature catalog (" + papers.size() + " papers)",
                    Map.of("query", ToolArgs.string(request.arguments(), "query", ""),
                            "papers", papers));
        } catch (Exception e) {
            return ToolResult.failure("Research literature lookup failed: " + e.getMessage(), e);
        }
    }
}
