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
import java.util.stream.Collectors;

@OloTool(
        id = CoreToolIds.RECENTLY_CHANGED_CODE,
        name = "Recently Changed Code",
        description = "Returns recently merged pull request code changes from files in a configured folder (GitHub API stub)",
        category = "observability",
        emoji = "🔀",
        tags = {"github", "pull-request", "code", "incident", "observability"},
        examples = {
            "Review recent payment-service merges before an outage",
            "Fetch diff for PR 839 that resized the retry thread pool"
        },
        arguments = {
            @OloProperty(
                    name = "limit",
                    label = "Limit",
                    type = OloPropertyType.NUMBER,
                    defaultValue = "5",
                    description = "Maximum number of recent change files to return",
                    group = "Query",
                    order = 0),
            @OloProperty(
                    name = "pullRequestNumber",
                    label = "Pull Request Number",
                    type = OloPropertyType.STRING,
                    description = "Optional filter — only return files whose name contains this PR number",
                    placeholder = "839",
                    group = "Query",
                    order = 1)
        },
        configuration = {
            @OloProperty(
                    name = "dataFolder",
                    label = "Changes Folder",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Folder containing pull request change files (.patch, .diff, .java, .json)",
                    placeholder = "demo-data/recent-changes",
                    group = "Data Source",
                    order = 0)
        })
@ToolId(CoreToolIds.RECENTLY_CHANGED_CODE)
@ImplementationId(CoreToolIds.RECENTLY_CHANGED_CODE)
public final class RecentlyChangedCodeTool implements Tool {

    static final String DEFAULT_DATA_FOLDER = "demo-data/recent-changes";

    @Override
    public String toolId() {
        return CoreToolIds.RECENTLY_CHANGED_CODE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            var folder = ObservabilitySupport.resolveDataFolder(request, "dataFolder", DEFAULT_DATA_FOLDER);
            int limit = RecentlyChangedCodeSupport.parseLimit(request);
            String pullRequestNumber = RecentlyChangedCodeSupport.parsePullRequestNumber(request);
            List<Map<String, Object>> changes =
                    RecentlyChangedCodeSupport.readChanges(folder, limit, pullRequestNumber);

            String content = changes.stream()
                    .map(change -> "--- " + change.get("fileName") + " ---"
                            + System.lineSeparator()
                            + change.get("content"))
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("dataFolder", folder.toString());
            output.put("limit", limit);
            if (!pullRequestNumber.isBlank()) {
                output.put("pullRequestNumber", pullRequestNumber);
            }
            output.put("changeCount", changes.size());
            output.put("changes", changes);
            output.put("content", content);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Recently changed code tool failed: " + e.getMessage(), e);
        }
    }
}
