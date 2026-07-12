/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.dynamicgraphcreation;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the {@code dynamic-graph-creation} workflow JSON under {@code olo-configuration/dynamic-graph-creation/}. */
public final class DynamicGraphCreationGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    public static void main(String[] args) throws IOException {
        Path configurationRoot = args.length > 0
                ? Path.of(args[0])
                : DynamicGraphCreationPaths.resolveConfigurationRoot();
        new DynamicGraphCreationGenerator().generate(configurationRoot);
    }

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        WorkflowDefinition workflow = DynamicGraphCreationDefinitions.dynamicGraphCreation();
        WorkflowValidator.validateOrThrow(workflow);
        Path target = configurationRoot.resolve(DynamicGraphCreationDefinitions.FILE_NAME + ".json");
        Files.writeString(target, json.serialize(workflow));
    }
}
