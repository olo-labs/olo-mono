package org.olo.core.tool;

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
        id = CoreToolIds.TRAVEL_OFFERS,
        name = "Travel offers",
        description = "Returns mock flight and hotel offers for travel planner scenarios",
        stability = OloStability.EXPERIMENTAL,
        category = "travel",
        emoji = "✈️",
        tags = {"travel", "offers", "scenario"},
        examples = {
            "Find weekend offers from London to Paris",
            "Compare hotel packages for Barcelona"
        },
        arguments = {
            @OloProperty(name = "origin", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "destination", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "query", type = OloPropertyType.STRING, required = false),
            @OloProperty(name = "limit", type = OloPropertyType.NUMBER, required = false, defaultValue = "3")
        },
        executionModel = OloExecutionModel.ACTIVITY)
@ToolId(CoreToolIds.TRAVEL_OFFERS)
@ImplementationId(CoreToolIds.TRAVEL_OFFERS)
public final class TravelOffersTool implements Tool {

    static final String DEMO_FOLDER = "demo-data/travel/offers";

    @Override
    public String toolId() {
        return CoreToolIds.TRAVEL_OFFERS;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            String query = ToolArgs.string(request.arguments(), "query", "");
            if (query.isBlank()) {
                String origin = ToolArgs.string(request.arguments(), "origin", "");
                String destination = ToolArgs.string(request.arguments(), "destination", "");
                query = (origin + " " + destination).trim();
            }
            Map<String, Object> args = new java.util.LinkedHashMap<>(request.arguments());
            args.put("query", query);
            ToolRequest enriched = new ToolRequest(
                    request.toolId(), request.invocationId(), args, request.configuration());
            List<Map<String, Object>> offers = ScenarioCatalogSupport.readCatalog(
                    DEMO_FOLDER, enriched, "query", "destination");
            return ToolResult.success(
                    "Travel offers catalog (" + offers.size() + " packages)",
                    Map.of("query", query, "offers", offers));
        } catch (Exception e) {
            return ToolResult.failure("Travel offers lookup failed: " + e.getMessage(), e);
        }
    }
}
