/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.Map;

/** Shared helpers for schema-only human-input plugins (not invoked during graph traversal). */
final class HumanInputPluginSupport {

    private HumanInputPluginSupport() {
    }

    static ToolResult schemaOnlyResult(String pluginId) {
        return ToolResult.success(Map.of("pluginRole", "human-input-schema", "pluginId", pluginId));
    }

    static ToolResult schemaOnlyInvoke(String pluginId, ToolRequest request, ExecutionContext context) {
        return schemaOnlyResult(pluginId);
    }
}
