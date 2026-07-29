/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.simplechatconversations;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the {@code simple-chat-conversations} pipeline collection. */
public final class SimpleChatConversationsGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    public static void main(String[] args) throws IOException {
        Path configurationRoot = args.length > 0
                ? Path.of(args[0])
                : SimpleChatConversationsPaths.resolveConfigurationRoot();
        new SimpleChatConversationsGenerator().generate(configurationRoot);
    }

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        for (String workflowId : SimpleChatConversationsDefinitions.workflowIds()) {
            writePreset(configurationRoot, workflowId, SimpleChatConversationsDefinitions.workflow(workflowId));
        }
    }

    private void writePreset(Path configurationRoot, String fileName, WorkflowDefinition workflow) throws IOException {
        WorkflowValidator.validateOrThrow(workflow);
        Files.writeString(configurationRoot.resolve(fileName + ".json"), json.serialize(workflow));
    }
}
