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
        id = CoreHumanInputPluginIds.RESTART_CONTAINER,
        name = "Restart Container Human Input",
        description = "Collects operator approval and restart-container arguments before the mock action runs",
        category = "human-input",
        emoji = "🔄",
        tags = {"human-input", "plugin", "container", "restart"},
        examples = {"Approve restart of payment-api-7f8c9d in production after triage"},
        arguments = {
            @OloProperty(
                    name = "approveRestart",
                    label = "Approve container restart?",
                    type = OloPropertyType.APPROVAL_TOGGLE,
                    required = true,
                    description = "Select Yes to authorize the restart",
                    group = "Approval",
                    order = 0),
            @OloProperty(
                    name = "scopeNotes",
                    label = "Scope notes",
                    type = OloPropertyType.TEXTAREA,
                    description = "Optional context or constraints for this run",
                    placeholder = "Latency spike on checkout path…",
                    group = "Scope",
                    order = 1),
            @OloProperty(
                    name = "containerId",
                    label = "Container ID",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Container or pod identifier to restart",
                    placeholder = "payment-api-7f8c9d",
                    group = "Restart action",
                    order = 2),
            @OloProperty(
                    name = "namespace",
                    label = "Namespace",
                    type = OloPropertyType.STRING,
                    required = true,
                    defaultValue = "production",
                    description = "Kubernetes namespace or deployment group",
                    placeholder = "production",
                    group = "Restart action",
                    order = 3)
        })
@ToolId(CoreHumanInputPluginIds.RESTART_CONTAINER)
@ImplementationId(CoreHumanInputPluginIds.RESTART_CONTAINER)
public final class RestartContainerHumanInputPlugin implements Tool {

    @Override
    public String toolId() {
        return CoreHumanInputPluginIds.RESTART_CONTAINER;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return HumanInputPluginSupport.schemaOnlyInvoke(toolId(), request, context);
    }
}
