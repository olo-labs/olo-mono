/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

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
        id = CoreHumanInputPluginIds.CREATE_PULL_REQUEST,
        name = "Create Pull Request Human Input",
        description = "Collects operator approval and create-pull-request arguments before the mock action runs",
        category = "human-input",
        emoji = "🔀",
        tags = {"human-input", "plugin", "git", "pull-request"},
        examples = {"Approve PR from feature/scenario-demo into main"},
        arguments = {
            @OloProperty(
                    name = "approvePullRequest",
                    label = "Approve pull request?",
                    type = OloPropertyType.APPROVAL_TOGGLE,
                    required = true,
                    description = "Select Yes to create the pull request",
                    group = "Approval",
                    order = 0),
            @OloProperty(
                    name = "scopeNotes",
                    label = "Scope notes",
                    type = OloPropertyType.TEXTAREA,
                    description = "Optional workflow or publishing constraints",
                    group = "Scope",
                    order = 1),
            @OloProperty(
                    name = "title",
                    label = "Pull request title",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Title for the pull request",
                    placeholder = "Scenario workflow update",
                    group = "Publish action",
                    order = 2),
            @OloProperty(
                    name = "headBranch",
                    label = "Head branch",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Source branch for the pull request",
                    placeholder = "feature/scenario-demo",
                    group = "Publish action",
                    order = 3)
        })
@ToolId(CoreHumanInputPluginIds.CREATE_PULL_REQUEST)
@ImplementationId(CoreHumanInputPluginIds.CREATE_PULL_REQUEST)
public final class CreatePullRequestHumanInputPlugin implements Tool {

    @Override
    public String toolId() {
        return CoreHumanInputPluginIds.CREATE_PULL_REQUEST;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return HumanInputPluginSupport.schemaOnlyInvoke(toolId(), request, context);
    }
}
