/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
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

import java.util.Map;

@OloTool(
        id = CoreToolIds.CONVERSATION_STORE,
        name = "Conversation Store",
        description = "Persists the current user message and workflow response to conversation history",
        category = "conversation",
        emoji = "🗂️",
        tags = {"conversation", "memory", "plugin", "store"},
        examples = {
            "Save operator triage outcome for the next turn",
            "Persist assistant response after scenario completion"
        },
        arguments = {
            @OloProperty(
                    name = "sessionId",
                    label = "Session ID",
                    type = OloPropertyType.STRING,
                    description = "Chat session identifier (defaults to workflow correlation/run id)",
                    placeholder = "demo-session",
                    group = "Session",
                    order = 0)
        })
@ToolId(CoreToolIds.CONVERSATION_STORE)
@ImplementationId(CoreToolIds.CONVERSATION_STORE)
public final class ConversationStoreTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.CONVERSATION_STORE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            Map<String, Object> output = ConversationPluginSupport.storeConversationTurn(request, context);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Conversation store failed: " + e.getMessage(), e);
        }
    }
}
