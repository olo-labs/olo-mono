package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloStability;
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
        id = CoreToolIds.WEB_SEARCH,
        name = "Web search",
        description = "Placeholder web search (connect olo-extensions for live results)",
        stability = OloStability.EXPERIMENTAL,
        category = "search",
        emoji = "🔍",
        tags = {"search", "core"},
        examples = {
            "Look up recent news on a topic",
            "Find documentation for an API",
            "Research competitors before a pitch"
        },
        arguments = @OloProperty(name = "query", type = OloPropertyType.STRING, required = true),
        capabilityInputs = {"query"},
        capabilityOutputs = {"results"})
@ToolId(CoreToolIds.WEB_SEARCH)
@ImplementationId(CoreToolIds.WEB_SEARCH)
public final class WebSearchTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.WEB_SEARCH;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String query = ToolArgs.string(request.arguments(), "query",
                ToolArgs.string(request.arguments(), "q", ""));
        if (query.isBlank()) {
            return ToolResult.failure("Web search requires query argument", null);
        }
        return ToolResult.success(
                "Web search stub — connect olo-extensions for live results",
                Map.of(
                        "query", query,
                        "results", List.of(Map.of(
                                "title", "Stub result for: " + query,
                                "url", "https://example.com/search?q=" + query))));
    }
}
