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
    void loadsInjectionEnvelopeFromCurrentActive(@TempDir Path scanFolder) throws Exception {
        Path source = Path.of("../olo-definition/olo-configuration/current-active/tool-call-agent-agent.json")
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(source)) {
            throw new org.opentest4j.TestAbortedException("tool-call injection sample not found");
        }

        Files.copy(source, scanFolder.resolve("tool-call-agent-agent.json"));

        WorkflowDefinitionRegistry registry = loader.load(scanFolder, false);

        assertThat(registry.findById("tool-call-agent")).isPresent();
        assertThat(registry.findByQueue("tool-call-agent")).isPresent();
    }
}
