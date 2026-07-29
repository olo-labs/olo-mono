/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.junit.jupiter.api.Test;
import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationPluginSupportTest {

    @Test
    void loadsHistoryAsReferenceOnlyAndStoresOnlyCurrentUserMessage() throws Exception {
        String sessionId = "conversation-reference-test-" + System.nanoTime();
        Path file = ConversationPluginSupport.resolveConversationFile(sessionId);
        Files.deleteIfExists(file);
        try {
            ConversationPluginSupport.writeHistory(sessionId, List.of(
                    turn("user", "explain newtons law"),
                    turn("assistant", "Newton's laws describe inertia, force, and action-reaction.")));

            DefaultExecutionContext context =
                    new DefaultExecutionContext("simple-chat", "run-1", "oloQueue2", "corr");
            ConversationLoadTool loadTool = new ConversationLoadTool();
            var loadResult = loadTool.invoke(
                    new ToolRequest(
                            CoreToolIds.CONVERSATION_LOAD,
                            "conversation-load",
                            Map.of("sessionId", sessionId, "message", "how to apply it?"),
                            Map.of()),
                    context);

            assertThat(loadResult.status()).isEqualTo(ToolStatus.SUCCESS);
            assertThat(context.getVariable(ConversationPluginSupport.CURRENT_MESSAGE_VARIABLE))
                    .isEqualTo("how to apply it?");
            assertThat(context.getVariable(ConversationPluginSupport.MESSAGE_VARIABLE).toString())
                    .contains("Current request:\nhow to apply it?")
                    .contains("Reference-only prior conversation context:")
                    .contains("Do not quote, summarize, mention, or answer the reference context")
                    .doesNotContain("Previous conversation summary:");

            context.setVariable(ConversationPluginSupport.RETURN_VALUE_VARIABLE, "Apply it by identifying forces.");
            ConversationStoreTool storeTool = new ConversationStoreTool();
            var storeResult = storeTool.invoke(
                    new ToolRequest(
                            CoreToolIds.CONVERSATION_STORE,
                            "conversation-store",
                            Map.of(
                                    "sessionId",
                                    sessionId,
                                    "message",
                                    context.getVariable(ConversationPluginSupport.MESSAGE_VARIABLE)),
                            Map.of()),
                    context);

            assertThat(storeResult.status()).isEqualTo(ToolStatus.SUCCESS);
            List<Map<String, Object>> history = ConversationPluginSupport.readHistory(sessionId);
            assertThat(history).hasSize(4);
            assertThat(history.get(2))
                    .containsEntry("role", "user")
                    .containsEntry("content", "how to apply it?");
            assertThat(String.valueOf(history.get(2).get("content")))
                    .doesNotContain("Reference-only prior conversation context")
                    .doesNotContain("Previous conversation summary");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static Map<String, Object> turn(String role, String content) {
        Map<String, Object> turn = new LinkedHashMap<>();
        turn.put("role", role);
        turn.put("content", content);
        return turn;
    }
}
