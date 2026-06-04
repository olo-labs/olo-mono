package io.olo.definition.samples;

import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.serializer.YamlWorkflowSerializer;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Writes canonical workflow samples (JSON and YAML) under {@code samples/}.
 * Invoked by the {@code generateSamples} Gradle task and {@link SampleWorkflowSerializationE2ETest}.
 */
public final class SampleWorkflowGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();
    private final YamlWorkflowSerializer yaml = new YamlWorkflowSerializer();

    public static void main(String[] args) throws IOException {
        Path samplesRoot = args.length > 0 ? Path.of(args[0]) : Path.of("samples");
        new SampleWorkflowGenerator().generate(samplesRoot);
    }

    public void generate(Path samplesRoot) throws IOException {
        writeSample(samplesRoot.resolve("minimal-echo"), "workflow", SampleWorkflowDefinitions::minimalEcho);
        writeSample(samplesRoot.resolve("stock-analysis"), "workflow", SampleWorkflowDefinitions::stockAnalysis);
        writeSample(samplesRoot.resolve("rag-chat"), "workflow", SampleWorkflowDefinitions::ragChat);
        writeSample(samplesRoot.resolve("condition-branch"), "workflow", SampleWorkflowDefinitions::conditionBranch);
        writeSample(
                samplesRoot.resolve("analysis-with-rag-extension"),
                "workflow-base",
                SampleWorkflowDefinitions::analysisBase);
        writeSample(
                samplesRoot.resolve("analysis-with-rag-extension"),
                "workflow-extended",
                SampleWorkflowDefinitions::analysisExtended);
    }

    private void writeSample(Path dir, String baseName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        WorkflowDefinition workflow = factory.get();
        WorkflowValidator.validateOrThrow(workflow);
        Files.createDirectories(dir);

        Path jsonFile = dir.resolve(baseName + ".json");
        Path yamlFile = dir.resolve(baseName + ".yaml");
        Files.writeString(jsonFile, json.serialize(workflow));
        Files.writeString(yamlFile, yaml.serialize(workflow));
    }
}
