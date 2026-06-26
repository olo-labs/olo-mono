package org.olo.bootstrap.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.bootstrap.exception.BootstrapException;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirectoryWorkflowDefinitionLoaderTest {

    private final DirectoryWorkflowDefinitionLoader loader = new DirectoryWorkflowDefinitionLoader();

    @Test
    void loadsMultipleVersionsForSameWorkflowId(@TempDir Path scanFolder) throws Exception {
        String fastJson = readPreset("fast.json");
        Files.writeString(scanFolder.resolve("fast-1.0.0.json"), fastJson);
        Files.writeString(
                scanFolder.resolve("fast-2.0.0.json"),
                fastJson.replaceFirst("\"version\" : \"1.0.0\"", "\"version\" : \"2.0.0\"")
                        .replaceFirst("\"isDefault\" : true", "\"isDefault\" : false"));

        WorkflowDefinitionRegistry registry = loader.load(scanFolder, false);

        assertThat(registry.getWorkflows()).hasSize(2);
        assertThat(registry.findByIdAndVersion("fast", "1.0.0")).isPresent();
        assertThat(registry.findByIdAndVersion("fast", "2.0.0")).isPresent();
        assertThat(registry.findById("fast")).get()
                .extracting(definition -> definition.getVersion())
                .isEqualTo("1.0.0");
        assertThat(registry.findByQueue("oloQueue2")).isPresent();
        assertThat(registry.findByIdAndVersion("fast", "9.9.9")).get()
                .extracting(definition -> definition.getVersion())
                .isEqualTo("1.0.0");
    }

    @Test
    void rejectsDuplicateIdAndVersion(@TempDir Path scanFolder) throws Exception {
        String fastJson = readPreset("fast.json");
        Files.writeString(scanFolder.resolve("a.json"), fastJson);
        Files.writeString(scanFolder.resolve("b.json"), fastJson);

        assertThatThrownBy(() -> loader.load(scanFolder, false))
                .isInstanceOf(BootstrapException.class)
                .hasMessageContaining("duplicate workflow id+version 'fast@1.0.0'");
    }

    @Test
    void allowsSharedQueueForDifferentWorkflowIds(@TempDir Path scanFolder) throws Exception {
        String fastJson = readPreset("fast.json");
        Files.writeString(scanFolder.resolve("fast.json"), fastJson);
        Files.writeString(
                scanFolder.resolve("planner.json"),
                readPreset("planner.json").replaceFirst("\"queue\" : \"oloQueue1\"", "\"queue\" : \"oloQueue2\""));

        WorkflowDefinitionRegistry registry = loader.load(scanFolder, false);

        assertThat(registry.findByQueue("oloQueue2")).isPresent();
        assertThat(registry.resolve("oloQueue2", "fast")).isPresent();
        assertThat(registry.resolve("oloQueue2", "planner")).isPresent();
    }

    private static String readPreset(String fileName) throws Exception {
        Path sourcePreset = Path.of("../olo-definition/olo-configuration/default", fileName)
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(sourcePreset)) {
            throw new org.opentest4j.TestAbortedException("preset not found at " + sourcePreset);
        }
        return Files.readString(sourcePreset);
    }
}
