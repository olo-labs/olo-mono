package org.olo.worker.config.source;

import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.ConfigurationFormat;
import org.olo.worker.config.model.ConfigurationSourceType;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.reader.WorkerConfigurationReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads worker configuration from a JSON or YAML file.
 */
public final class FileConfigurationSource implements ConfigurationSource {

    private final Path path;
    private final WorkerConfigurationReader reader;

    public FileConfigurationSource(Path path) {
        this(path, new WorkerConfigurationReader());
    }

    public FileConfigurationSource(Path path, WorkerConfigurationReader reader) {
        this.path = Objects.requireNonNull(path, "path");
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    @Override
    public ConfigurationSourceType getSourceType() {
        return ConfigurationSourceType.FILE;
    }

    @Override
    public String getSourceId() {
        return path.toAbsolutePath().normalize().toString();
    }

    @Override
    public WorkerConfiguration load() {
        if (!Files.exists(path)) {
            throw new WorkerConfigurationException("configuration file does not exist: " + path);
        }
        if (Files.isDirectory(path)) {
            throw new WorkerConfigurationException("configuration path must be a file, not a directory: " + path);
        }
        if (ConfigurationFormat.fromPath(path).isEmpty()) {
            throw new WorkerConfigurationException(
                    "unsupported configuration file extension: " + path.getFileName());
        }
        return reader.read(path);
    }
}
