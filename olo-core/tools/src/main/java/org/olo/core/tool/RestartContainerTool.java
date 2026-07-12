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
        id = CoreToolIds.RESTART_CONTAINER,
        name = "Restart Container",
        description = "Mock restart of a container/pod after human approval (writes execution log)",
        category = "action",
        emoji = "🔄",
        tags = {"container", "restart", "ops", "human-approved", "mock"},
        examples = {
            "Restart payment-service pod payment-api-7f8c9 after incident RCA",
            "Roll payment-gateway container payment-gw-2 in namespace production"
        },
        arguments = {
            @OloProperty(
                    name = "containerId",
                    label = "Container ID",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Container or pod identifier to restart",
                    placeholder = "payment-api-7f8c9d",
                    group = "Target",
                    order = 0),
            @OloProperty(
                    name = "namespace",
                    label = "Namespace",
                    type = OloPropertyType.STRING,
                    defaultValue = "default",
                    description = "Kubernetes namespace or deployment group",
                    placeholder = "production",
                    group = "Target",
                    order = 1)
        })
@ToolId(CoreToolIds.RESTART_CONTAINER)
@ImplementationId(CoreToolIds.RESTART_CONTAINER)
public final class RestartContainerTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.RESTART_CONTAINER;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String containerId = ToolArgs.string(request.arguments(), "containerId", "");
        if (containerId.isBlank()) {
            return ToolResult.failure("containerId is required for restart-container mock action", null);
        }
        String namespace = ToolArgs.string(request.arguments(), "namespace", "default");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("containerId", containerId);
        arguments.put("namespace", namespace);
        try {
            Map<String, Object> output =
                    MockActionLogSupport.recordMockExecution(context, toolId(), "RESTART_CONTAINER", arguments);
            output.put("restartStatus", "SCHEDULED");
            output.put("summary", "Mock restart scheduled for " + namespace + "/" + containerId);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Restart container mock action failed: " + e.getMessage(), e);
        }
    }
}
