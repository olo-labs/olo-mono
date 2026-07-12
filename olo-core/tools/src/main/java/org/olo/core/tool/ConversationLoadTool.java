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
        id = CoreToolIds.CONVERSATION_LOAD,
        name = "Conversation Load",
        description = "Loads prior conversation turns and attaches a summary to the workflow message",
        category = "conversation",
        emoji = "💬",
        tags = {"conversation", "memory", "plugin", "summary"},
        examples = {
            "Restore chat context for a returning operator session",
            "Attach prior incident triage summary before the planner runs"
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
@ToolId(CoreToolIds.CONVERSATION_LOAD)
@ImplementationId(CoreToolIds.CONVERSATION_LOAD)
public final class ConversationLoadTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.CONVERSATION_LOAD;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        try {
            String currentMessage = ToolArgs.string(request.arguments(), ConversationPluginSupport.MESSAGE_VARIABLE, "");
            if (currentMessage.isBlank()) {
                Object messageVariable = context.getVariable(ConversationPluginSupport.MESSAGE_VARIABLE);
                currentMessage = messageVariable == null ? "" : String.valueOf(messageVariable);
            }
            Map<String, Object> output =
                    ConversationPluginSupport.loadConversationContext(request, context, currentMessage);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Conversation load failed: " + e.getMessage(), e);
        }
    }
}
