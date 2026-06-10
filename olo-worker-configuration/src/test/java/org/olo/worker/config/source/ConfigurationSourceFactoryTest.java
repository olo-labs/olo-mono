package org.olo.worker.config.source;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationSourceFactoryTest {

    @Test
    void resolvesMonorepoSampleWhenDefaultMissing() {
        Path sample = Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize();
        if (!Files.exists(sample)) {
            throw new org.opentest4j.TestAbortedException("sample config not found at " + sample);
        }

        Path resolved = ConfigurationSourceFactory.resolveFilePath();

        assertThat(Files.exists(resolved)).isTrue();
        assertThat(resolved.getFileName().toString()).startsWith("worker-config.");
    }
}
