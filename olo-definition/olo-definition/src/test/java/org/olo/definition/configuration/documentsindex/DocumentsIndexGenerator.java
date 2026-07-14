/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsindex;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the {@code documents-index} RAG vector ingest pipeline JSON. */
public final class DocumentsIndexGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        WorkflowDefinition workflow = DocumentsIndexDefinitions.documentsIndex();
        WorkflowValidator.validateOrThrow(workflow);
        String serialized = json.serialize(workflow);
        Files.writeString(
                configurationRoot.resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json"),
                serialized);
        syncToCurrentActive(configurationRoot, serialized);
    }

    private static void syncToCurrentActive(Path configurationRoot, String serialized) throws IOException {
        Path currentActive = configurationRoot.getParent().resolve("current-active");
        if (!Files.isDirectory(currentActive)) {
            return;
        }
        Files.writeString(
                currentActive.resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json"),
                serialized);
    }

    public void generateRoot(String[] args) throws IOException {
        Path configurationRoot = args.length > 0 ? Path.of(args[0]) : DocumentsIndexPaths.resolveConfigurationRoot();
        generate(configurationRoot);
    }

    public static void main(String[] args) throws Exception {
        new DocumentsIndexGenerator().generateRoot(args);
    }
}
