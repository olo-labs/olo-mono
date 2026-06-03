package io.olo.definition.samples;

import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.serializer.WorkflowSerializer;
import io.olo.definition.serializer.YamlWorkflowSerializer;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: build sample workflows in code, serialize, deserialize, validate equality.
 */
class SampleWorkflowSerializationE2ETest {

    private final JsonWorkflowSerializer jsonSerializer = new JsonWorkflowSerializer();
    private final YamlWorkflowSerializer yamlSerializer = new YamlWorkflowSerializer();

    @ParameterizedTest(name = "{0} via JSON")
    @MethodSource("sampleWorkflows")
    void jsonRoundTripPreservesWorkflow(String sampleName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        roundTrip(sampleName, factory, jsonSerializer);
    }

    @ParameterizedTest(name = "{0} via YAML")
    @MethodSource("sampleWorkflows")
    void yamlRoundTripPreservesWorkflow(String sampleName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        roundTrip(sampleName, factory, yamlSerializer);
    }

    @ParameterizedTest(name = "{0} JSON bytes")
    @MethodSource("sampleWorkflows")
    void jsonBytesRoundTripPreservesWorkflow(String sampleName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        WorkflowDefinition original = buildAndValidate(sampleName, factory);

        byte[] bytes = jsonSerializer.serializeToBytes(original);
        assertThat(bytes).isNotEmpty();

        WorkflowDefinition restored = jsonSerializer.deserialize(bytes);
        assertThat(restored).isEqualTo(original);
        assertThat(WorkflowValidator.validate(restored).valid()).isTrue();
    }

    /**
     * Builds each sample in code, serializes to {@code samples/generated/}, then round-trips from disk.
     */
    @Test
    void buildSerializeToFilesAndDeserialize() throws IOException {
        String samplesDir = System.getProperty("olo.samples.dir", "samples");
        Path samplesRoot = Path.of(samplesDir);
        Path generatedRoot = samplesRoot.resolve("generated");
        Files.createDirectories(generatedRoot);

        Map<String, Supplier<WorkflowDefinition>> extras = Map.of(
                "analysis-with-rag-extension/workflow-base", SampleWorkflowDefinitions::analysisBase,
                "analysis-with-rag-extension/workflow-extended", SampleWorkflowDefinitions::analysisExtended);

        for (Arguments args : sampleWorkflows().toList()) {
            String sampleName = (String) args.get()[0];
            @SuppressWarnings("unchecked")
            Supplier<WorkflowDefinition> factory = (Supplier<WorkflowDefinition>) args.get()[1];
            writeAndVerify(generatedRoot.resolve(sampleName), factory);
        }
        for (Map.Entry<String, Supplier<WorkflowDefinition>> entry : extras.entrySet()) {
            writeAndVerify(generatedRoot.resolve(entry.getKey()), entry.getValue());
        }
    }

    private void writeAndVerify(Path sampleDir, Supplier<WorkflowDefinition> factory) throws IOException {
        WorkflowDefinition built = factory.get();
        WorkflowValidator.validateOrThrow(built);

        Files.createDirectories(sampleDir);
        Path jsonFile = sampleDir.resolve("workflow.json");
        Path yamlFile = sampleDir.resolve("workflow.yaml");

        Files.writeString(jsonFile, jsonSerializer.serialize(built));
        Files.writeString(yamlFile, yamlSerializer.serialize(built));

        WorkflowDefinition fromJson = jsonSerializer.deserialize(Files.readString(jsonFile));
        WorkflowDefinition fromYaml = yamlSerializer.deserialize(Files.readString(yamlFile));

        assertThat(fromJson).isEqualTo(built);
        assertThat(fromYaml).isEqualTo(built);
        assertThat(WorkflowValidator.validate(fromJson).valid()).isTrue();
    }

    @ParameterizedTest(name = "{0} cross-format JSON→YAML")
    @MethodSource("sampleWorkflows")
    void jsonToYamlCrossFormatPreservesWorkflow(String sampleName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        WorkflowDefinition original = buildAndValidate(sampleName, factory);

        String json = jsonSerializer.serialize(original);
        WorkflowDefinition fromJson = jsonSerializer.deserialize(json);

        String yaml = yamlSerializer.serialize(fromJson);
        WorkflowDefinition fromYaml = yamlSerializer.deserialize(yaml);

        assertThat(fromYaml).isEqualTo(original);
    }

    private void roundTrip(
            String sampleName,
            Supplier<WorkflowDefinition> factory,
            WorkflowSerializer serializer)
            throws IOException {
        WorkflowDefinition original = buildAndValidate(sampleName, factory);

        String serialized = serializer.serialize(original);
        assertThat(serialized).isNotBlank();

        WorkflowDefinition restored = serializer.deserialize(serialized);

        assertThat(restored)
                .as("deserialized workflow must match original for sample '%s'", sampleName)
                .isEqualTo(original);
        assertThat(WorkflowValidator.validate(restored).valid())
                .as("restored workflow must pass validation for sample '%s'", sampleName)
                .isTrue();
    }

    private static WorkflowDefinition buildAndValidate(
            String sampleName, Supplier<WorkflowDefinition> factory) {
        WorkflowDefinition original = factory.get();
        assertThat(WorkflowValidator.validate(original).valid())
                .as("built workflow must be valid for sample '%s'", sampleName)
                .isTrue();
        return original;
    }

    static Stream<Arguments> sampleWorkflows() {
        return Stream.of(
                Arguments.of("minimal-echo", (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::minimalEcho),
                Arguments.of(
                        "stock-analysis",
                        (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::stockAnalysis),
                Arguments.of("rag-chat", (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::ragChat),
                Arguments.of(
                        "analysis-base",
                        (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::analysisBase),
                Arguments.of(
                        "analysis-extended",
                        (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::analysisExtended),
                Arguments.of(
                        "condition-branch",
                        (Supplier<WorkflowDefinition>) SampleWorkflowDefinitions::conditionBranch));
    }
}
