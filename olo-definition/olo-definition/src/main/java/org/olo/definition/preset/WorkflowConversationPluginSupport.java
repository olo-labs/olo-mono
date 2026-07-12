/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.preset;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.Map;

/** Conversation load/store plugins wired at the beginning and end of agent-style workflows. */
public final class WorkflowConversationPluginSupport {

    public static final String CONVERSATION_LOAD_TOOL_ID = "olo-core:conversation-load";
    public static final String CONVERSATION_STORE_TOOL_ID = "olo-core:conversation-store";
    public static final String CONVERSATION_LOAD_NODE_ID = "conversation-load";
    public static final String CONVERSATION_STORE_NODE_ID = "conversation-store";
    public static final String CONVERSATION_SUMMARY_VARIABLE = "conversationSummary";
    public static final String CONVERSATION_HISTORY_VARIABLE = "conversationHistory";

    private WorkflowConversationPluginSupport() {
    }

    public static VariableDefinition conversationSummaryVariable() {
        return VariableDefinition.builder()
                .name(CONVERSATION_SUMMARY_VARIABLE)
                .type("string")
                .description("Summary of prior conversation turns loaded by the conversation-load plugin")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "conversation-summary"))
                .build();
    }

    public static VariableDefinition conversationHistoryVariable() {
        return VariableDefinition.builder()
                .name(CONVERSATION_HISTORY_VARIABLE)
                .type("string")
                .description("JSON array of prior conversation turns loaded by the conversation-load plugin")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "conversation-history"))
                .build();
    }

    public static ToolDefinition conversationLoadTool() {
        return ToolDefinition.builder()
                .id(CONVERSATION_LOAD_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("Conversation Load")
                        .description("Loads prior conversation and attaches summary to the workflow message")
                        .addExample("Restore operator chat context before triage begins")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CONVERSATION_LOAD_TOOL_ID)
                        .build())
                .build();
    }

    public static ToolDefinition conversationStoreTool() {
        return ToolDefinition.builder()
                .id(CONVERSATION_STORE_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("Conversation Store")
                        .description("Persists the current turn to conversation history for the next run")
                        .addExample("Save assistant response after scenario completion")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CONVERSATION_STORE_TOOL_ID)
                        .build())
                .build();
    }

    public static WorkflowBuilder registerConversationTools(WorkflowBuilder builder) {
        return builder.tool(conversationLoadTool()).tool(conversationStoreTool());
    }

    public static WorkflowBuilder conversationLoadToolNode(WorkflowBuilder builder) {
        return builder.toolNode(CONVERSATION_LOAD_NODE_ID)
                .putNodeConfiguration(
                        CONVERSATION_LOAD_NODE_ID, Map.of("toolId", CONVERSATION_LOAD_TOOL_ID));
    }

    public static WorkflowBuilder conversationStoreToolNode(WorkflowBuilder builder) {
        return builder.toolNode(CONVERSATION_STORE_NODE_ID)
                .putNodeConfiguration(
                        CONVERSATION_STORE_NODE_ID, Map.of("toolId", CONVERSATION_STORE_TOOL_ID));
    }

    public static WorkflowBuilder wireConversationToolNodes(WorkflowBuilder builder) {
        return conversationStoreToolNode(conversationLoadToolNode(registerConversationTools(builder)));
    }

    public static String conversationContextPromptBlock() {
        return """

                Previous conversation summary (from conversation-load plugin):
                {conversationSummary}
                """;
    }
}
