package org.olo.bootstrap.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSubgraphInjectionLoaderTest {

    private final DirectoryWorkflowDefinitionLoader loader = new DirectoryWorkflowDefinitionLoader();

    @Test
    void loadsDefaultAgentPresetFromConfiguration(@TempDir Path scanFolder) throws Exception {
        Path agent = resolveConfigurationFile("default", "agent.json");
        Files.copy(agent, scanFolder.resolve("agent.json"));

        WorkflowDefinitionRegistry registry = loader.load(scanFolder, false);

        assertThat(registry.findById("agent")).isPresent();
        assertThat(registry.findByQueue("oloQueue2")).isPresent();
    }

    private static Path resolveConfigurationFile(String folder, String fileName) {
        for (String base :
                new String[] {
                    "../olo-definition/olo-configuration",
                    "olo-definition/olo-configuration",
                    "../olo-configuration"
                }) {
            Path file = Path.of(base, folder, fileName).toAbsolutePath().normalize();
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        throw new org.opentest4j.TestAbortedException("configuration file not found: " + folder + "/" + fileName);
    }
}
