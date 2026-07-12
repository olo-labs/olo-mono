/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.core.tool.action.MockActionLogSupport;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@OloTool(
        id = CoreToolIds.CREATE_PULL_REQUEST,
        name = "Create Pull Request",
        description = "Mock pull request creation after human approval (writes execution log)",
        category = "action",
        emoji = "🔀",
        tags = {"github", "pull-request", "human-approved", "mock"},
        examples = {
            "Open hotfix PR for payment-service timeout patch",
            "Create PR from feature/workflow-graph to main with generated workflow"
        },
        arguments = {
            @OloProperty(
                    name = "title",
                    label = "PR Title",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Pull request title",
                    placeholder = "fix: restore payment gateway timeouts",
                    group = "Pull Request",
                    order = 0),
            @OloProperty(
                    name = "headBranch",
                    label = "Head Branch",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Source branch for the pull request",
                    placeholder = "hotfix/payment-timeout",
                    group = "Pull Request",
                    order = 1),
            @OloProperty(
                    name = "baseBranch",
                    label = "Base Branch",
                    type = OloPropertyType.STRING,
                    defaultValue = "main",
                    description = "Target branch for the pull request",
                    placeholder = "main",
                    group = "Pull Request",
                    order = 2),
            @OloProperty(
                    name = "repository",
                    label = "Repository",
                    type = OloPropertyType.STRING,
                    defaultValue = "olo-labs/payment-service",
                    description = "Repository slug",
                    group = "Pull Request",
                    order = 3)
        })
@ToolId(CoreToolIds.CREATE_PULL_REQUEST)
@ImplementationId(CoreToolIds.CREATE_PULL_REQUEST)
public final class CreatePullRequestTool implements Tool {

    private static final AtomicInteger PR_COUNTER = new AtomicInteger(940);

    @Override
    public String toolId() {
        return CoreToolIds.CREATE_PULL_REQUEST;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String title = ToolArgs.string(request.arguments(), "title", "");
        String headBranch = ToolArgs.string(request.arguments(), "headBranch", "");
        if (title.isBlank() || headBranch.isBlank()) {
            return ToolResult.failure("title and headBranch are required for create-pull-request mock action", null);
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("title", title);
        arguments.put("headBranch", headBranch);
        arguments.put("baseBranch", ToolArgs.string(request.arguments(), "baseBranch", "main"));
        arguments.put("repository", ToolArgs.string(request.arguments(), "repository", "olo-labs/payment-service"));
        int prNumber = PR_COUNTER.incrementAndGet();
        try {
            Map<String, Object> output =
                    MockActionLogSupport.recordMockExecution(context, toolId(), "CREATE_PULL_REQUEST", arguments);
            output.put("pullRequestNumber", prNumber);
            output.put("pullRequestUrl", "https://github.example/mock/pull/" + prNumber);
            output.put("summary", "Mock PR #" + prNumber + " opened: " + title);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Create pull request mock action failed: " + e.getMessage(), e);
        }
    }
}
