/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Base generator for scenario collections under {@code olo-configuration/<scenario>/}. */
public class ScenarioConfigurationGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();
    private final List<ScenarioEntry> entries;

    protected ScenarioConfigurationGenerator(String orchestratorId, Supplier<WorkflowDefinition> orchestrator, ScenarioEntry... children) {
        this.entries = new java.util.ArrayList<>();
        this.entries.add(new ScenarioEntry(orchestratorId, orchestrator));
        this.entries.addAll(Arrays.asList(children));
    }

    protected static ScenarioEntry entry(String fileName, Supplier<WorkflowDefinition> factory) {
        return new ScenarioEntry(fileName, factory);
    }

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        for (ScenarioEntry entry : entries) {
            WorkflowDefinition workflow = entry.factory().get();
            WorkflowValidator.validateOrThrow(workflow);
            Files.writeString(configurationRoot.resolve(entry.fileName() + ".json"), json.serialize(workflow));
        }
    }

    public void generateRoot(String[] args, Supplier<Path> defaultRoot) throws IOException {
        Path configurationRoot = args.length > 0 ? Path.of(args[0]) : defaultRoot.get();
        generate(configurationRoot);
    }

    public List<ScenarioEntry> entries() {
        return List.copyOf(entries);
    }

    public record ScenarioEntry(String fileName, Supplier<WorkflowDefinition> factory) {
    }
}
