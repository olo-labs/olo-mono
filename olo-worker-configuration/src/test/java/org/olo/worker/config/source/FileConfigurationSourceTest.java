package org.olo.worker.config.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.loader.DefaultWorkerConfigurationLoader;
import org.olo.worker.config.model.ConfigurationSourceType;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.reader.WorkerConfigurationReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileConfigurationSourceTest {

    @Test
    void loadsYamlSample() {
        Path config = Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize();

        WorkerConfiguration configuration = DefaultWorkerConfigurationLoader
                .fromSource(new FileConfigurationSource(config))
                .load();

        assertThat(configuration.getId()).isEqualTo("default-worker");
        assertThat(configuration.getServer().getPort()).isEqualTo(8080);
        assertThat(configuration.getServer().getHost()).isEqualTo("0.0.0.0");
        assertThat(configuration.getWorkflowDefinitions().getScanFolder())
                .isEqualTo("../../olo-definition/olo-configuration/current-active");
        assertThat(configuration.getWorkflowDefinitions().getRecursive()).isFalse();
        assertThat(configuration.getInput().resolveMaxLocalMessageSize()).isEqualTo(50);
        assertThat(configuration.getCache().isEnabled()).isTrue();
    }

    @Test
    void loadsJsonSample() {
        Path config = Paths.get("samples/worker-config.json").toAbsolutePath().normalize();

        WorkerConfiguration configuration = DefaultWorkerConfigurationLoader
                .fromSource(new FileConfigurationSource(config))
                .load();

        assertThat(configuration.getId()).isEqualTo("default-worker");
        assertThat(configuration.getServer().getPort()).isEqualTo(8080);
    }

    @Test
    void roundTripsYaml(@TempDir Path tempDir) throws Exception {
        Path source = Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize();
        WorkerConfigurationReader reader = new WorkerConfigurationReader();
        WorkerConfiguration original = reader.read(source);
        String yaml = reader.write(original, org.olo.worker.config.model.ConfigurationFormat.YAML);

        Path target = tempDir.resolve("roundtrip.yaml");
        Files.writeString(target, yaml);
        WorkerConfiguration copy = reader.read(target);

        assertThat(copy).isEqualTo(original);
    }

    @Test
    void rejectsMissingFile() {
        assertThatThrownBy(() -> new FileConfigurationSource(Path.of("missing.yaml")).load())
                .isInstanceOf(WorkerConfigurationException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void rejectsDirectory(@TempDir Path tempDir) {
        assertThatThrownBy(() -> new FileConfigurationSource(tempDir).load())
                .isInstanceOf(WorkerConfigurationException.class)
                .hasMessageContaining("must be a file");
    }

    @Test
    void reportsFileSourceType() {
        Path config = Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize();
        FileConfigurationSource source = new FileConfigurationSource(config);

        assertThat(source.getSourceType()).isEqualTo(ConfigurationSourceType.FILE);
        assertThat(source.getSourceId()).endsWith("worker-config.yaml");
    }
}
