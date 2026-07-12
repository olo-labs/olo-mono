/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.researchplanner;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Writes the {@code research-planner} scenario collection under {@code olo-configuration/research-planner/}. */
public final class ResearchPlannerGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    private static final List<ScenarioEntry> ENTRIES = List.of(
            new ScenarioEntry(ResearchPlannerDefinitions.ORCHESTRATOR_ID, ResearchPlannerDefinitions::orchestrator),
            new ScenarioEntry(ResearchPlannerDefinitions.LITERATURE_AGENT_ID, ResearchPlannerDefinitions::literatureAgent),
            new ScenarioEntry(ResearchPlannerDefinitions.SYNTHESIS_AGENT_ID, ResearchPlannerDefinitions::synthesisAgent));

    public static void main(String[] args) throws IOException {
        Path configurationRoot = args.length > 0
                ? Path.of(args[0])
                : ResearchPlannerPaths.resolveConfigurationRoot();
        new ResearchPlannerGenerator().generate(configurationRoot);
    }

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        for (ScenarioEntry entry : ENTRIES) {
            writePreset(configurationRoot, entry.fileName(), entry.factory());
        }
    }

    private void writePreset(Path configurationRoot, String fileName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        WorkflowDefinition workflow = factory.get();
        WorkflowValidator.validateOrThrow(workflow);
        Path target = configurationRoot.resolve(fileName + ".json");
        Files.writeString(target, json.serialize(workflow));
    }

    private record ScenarioEntry(String fileName, Supplier<WorkflowDefinition> factory) {
    }
}
