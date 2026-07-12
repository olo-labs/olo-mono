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
        id = CoreHumanInputPluginIds.GIT_REVERT,
        name = "Git Revert Human Input",
        description = "Collects operator approval and git-revert arguments before the mock action runs",
        category = "human-input",
        emoji = "⏪",
        tags = {"human-input", "plugin", "git", "revert"},
        examples = {"Approve revert of commit abc123def on main"},
        arguments = {
            @OloProperty(
                    name = "approveRevert",
                    label = "Approve git revert?",
                    type = OloPropertyType.APPROVAL_TOGGLE,
                    required = true,
                    description = "Select Yes to authorize the revert",
                    group = "Approval",
                    order = 0),
            @OloProperty(
                    name = "scopeNotes",
                    label = "Scope notes",
                    type = OloPropertyType.TEXTAREA,
                    description = "Optional release or rollback context",
                    group = "Scope",
                    order = 1),
            @OloProperty(
                    name = "commitSha",
                    label = "Commit SHA",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Commit to revert",
                    placeholder = "abc123def",
                    group = "Revert action",
                    order = 2),
            @OloProperty(
                    name = "branch",
                    label = "Branch",
                    type = OloPropertyType.STRING,
                    required = true,
                    defaultValue = "main",
                    description = "Branch to revert on",
                    placeholder = "main",
                    group = "Revert action",
                    order = 3)
        })
@ToolId(CoreHumanInputPluginIds.GIT_REVERT)
@ImplementationId(CoreHumanInputPluginIds.GIT_REVERT)
public final class GitRevertHumanInputPlugin implements Tool {

    @Override
    public String toolId() {
        return CoreHumanInputPluginIds.GIT_REVERT;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return HumanInputPluginSupport.schemaOnlyInvoke(toolId(), request, context);
    }
}
