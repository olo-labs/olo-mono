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

@OloTool(
        id = CoreToolIds.GIT_REVERT,
        name = "Git Revert",
        description = "Mock git revert of a commit after human approval (writes execution log)",
        category = "action",
        emoji = "⏪",
        tags = {"git", "revert", "rollback", "human-approved", "mock"},
        examples = {
            "Revert commit a1b2c3d on main after failed release",
            "Rollback hotfix commit on release/2.4 branch"
        },
        arguments = {
            @OloProperty(
                    name = "commitSha",
                    label = "Commit SHA",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Commit to revert",
                    placeholder = "a1b2c3d4e5f6",
                    group = "Git",
                    order = 0),
            @OloProperty(
                    name = "branch",
                    label = "Branch",
                    type = OloPropertyType.STRING,
                    defaultValue = "main",
                    description = "Branch where the revert commit will be created",
                    placeholder = "main",
                    group = "Git",
                    order = 1),
            @OloProperty(
                    name = "repository",
                    label = "Repository",
                    type = OloPropertyType.STRING,
                    defaultValue = "olo-labs/payment-service",
                    description = "Repository slug for the revert",
                    group = "Git",
                    order = 2)
        })
@ToolId(CoreToolIds.GIT_REVERT)
@ImplementationId(CoreToolIds.GIT_REVERT)
public final class GitRevertTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.GIT_REVERT;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String commitSha = ToolArgs.string(request.arguments(), "commitSha", "");
        if (commitSha.isBlank()) {
            return ToolResult.failure("commitSha is required for git-revert mock action", null);
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("commitSha", commitSha);
        arguments.put("branch", ToolArgs.string(request.arguments(), "branch", "main"));
        arguments.put("repository", ToolArgs.string(request.arguments(), "repository", "olo-labs/payment-service"));
        try {
            Map<String, Object> output =
                    MockActionLogSupport.recordMockExecution(context, toolId(), "GIT_REVERT", arguments);
            output.put("revertCommitSha", "mock-revert-" + commitSha.substring(0, Math.min(7, commitSha.length())));
            output.put("summary", "Mock revert created on " + arguments.get("branch"));
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Git revert mock action failed: " + e.getMessage(), e);
        }
    }
}
