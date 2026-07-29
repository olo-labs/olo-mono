/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsdelete;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the {@code documents-delete} RAG vector delete pipeline JSON. */
public final class DocumentsDeleteGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        WorkflowDefinition workflow = DocumentsDeleteDefinitions.documentsDelete();
        WorkflowValidator.validateOrThrow(workflow);
        String serialized = json.serialize(workflow);
        Files.writeString(
                configurationRoot.resolve(DocumentsDeleteDefinitions.PIPELINE_ID + ".json"),
                serialized);
        syncToCurrentActive(configurationRoot, serialized);
    }

    private static void syncToCurrentActive(Path configurationRoot, String serialized) throws IOException {
        Path currentActive = configurationRoot.getParent().resolve("current-active");
        if (!Files.isDirectory(currentActive)) {
            return;
        }
        Files.writeString(
                currentActive.resolve(DocumentsDeleteDefinitions.PIPELINE_ID + ".json"),
                serialized);
    }

    public void generateRoot(String[] args) throws IOException {
        Path configurationRoot = args.length > 0 ? Path.of(args[0]) : DocumentsDeletePaths.resolveConfigurationRoot();
        generate(configurationRoot);
    }

    public static void main(String[] args) throws Exception {
        new DocumentsDeleteGenerator().generateRoot(args);
    }
}
