package org.olo.definition.configuration.travelplanner;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Writes the {@code travel-planner} scenario collection under {@code olo-configuration/travel-planner/}. */
public final class TravelPlannerGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    private static final List<ScenarioEntry> ENTRIES = List.of(
            new ScenarioEntry(TravelPlannerDefinitions.ORCHESTRATOR_ID, TravelPlannerDefinitions::orchestrator),
            new ScenarioEntry(TravelPlannerDefinitions.DESTINATION_AGENT_ID, TravelPlannerDefinitions::destinationAgent),
            new ScenarioEntry(TravelPlannerDefinitions.ITINERARY_AGENT_ID, TravelPlannerDefinitions::itineraryAgent));

    public static void main(String[] args) throws IOException {
        Path configurationRoot = args.length > 0
                ? Path.of(args[0])
                : TravelPlannerPaths.resolveConfigurationRoot();
        new TravelPlannerGenerator().generate(configurationRoot);
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
