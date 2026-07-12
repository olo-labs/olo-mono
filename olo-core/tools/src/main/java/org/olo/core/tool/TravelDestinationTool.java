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
        id = CoreToolIds.TRAVEL_DESTINATIONS,
        name = "Travel destinations",
        description = "Returns mock destination guides for travel planner scenarios",
        stability = OloStability.EXPERIMENTAL,
        category = "travel",
        emoji = "🌍",
        tags = {"travel", "destinations", "scenario"},
        examples = {
            "Suggest destinations in Europe for summer",
            "Compare city break options in France"
        },
        arguments = {
            @OloProperty(name = "query", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "region", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "limit", type = OloPropertyType.NUMBER, required = false, defaultValue = "3")
        },
        executionModel = OloExecutionModel.ACTIVITY)
@ToolId(CoreToolIds.TRAVEL_DESTINATIONS)
@ImplementationId(CoreToolIds.TRAVEL_DESTINATIONS)
public final class TravelDestinationTool implements Tool {

    static final String DEMO_FOLDER = "demo-data/travel/destinations";

    @Override
    public String toolId() {
        return CoreToolIds.TRAVEL_DESTINATIONS;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            List<Map<String, Object>> destinations = ScenarioCatalogSupport.readCatalog(
                    DEMO_FOLDER, request, "query", "region");
            return ToolResult.success(
                    "Travel destination catalog (" + destinations.size() + " entries)",
                    Map.of("query", ToolArgs.string(request.arguments(), "query", ""),
                            "destinations", destinations));
        } catch (Exception e) {
            return ToolResult.failure("Travel destination lookup failed: " + e.getMessage(), e);
        }
    }
}
