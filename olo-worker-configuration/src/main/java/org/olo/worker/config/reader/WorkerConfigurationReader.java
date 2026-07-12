/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.ConfigurationFormat;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.serializer.JacksonWorkerConfigurationMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Deserializes {@link WorkerConfiguration} from JSON or YAML content.
 */
public final class WorkerConfigurationReader {

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public WorkerConfigurationReader() {
        this(JacksonWorkerConfigurationMapper.jsonMapper(), JacksonWorkerConfigurationMapper.yamlMapper());
    }

    public WorkerConfigurationReader(ObjectMapper jsonMapper, ObjectMapper yamlMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.yamlMapper = Objects.requireNonNull(yamlMapper, "yamlMapper");
    }

    public WorkerConfiguration read(Path path) {
        Objects.requireNonNull(path, "path");
        ConfigurationFormat format = ConfigurationFormat.fromPath(path)
                .orElseThrow(() -> new WorkerConfigurationException(
                        "unsupported configuration file extension: " + path.getFileName()));
        try {
            return read(Files.readString(path), format);
        } catch (IOException e) {
            throw new WorkerConfigurationException("failed to read configuration from " + path, e);
        }
    }

    public WorkerConfiguration read(String content, ConfigurationFormat format) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(format, "format");
        try {
            return mapperFor(format).readValue(content, WorkerConfiguration.class);
        } catch (IOException e) {
            throw new WorkerConfigurationException("failed to deserialize worker configuration (" + format + ")", e);
        }
    }

    public String write(WorkerConfiguration configuration, ConfigurationFormat format) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(format, "format");
        try {
            return mapperFor(format).writerWithDefaultPrettyPrinter().writeValueAsString(configuration);
        } catch (IOException e) {
            throw new WorkerConfigurationException("failed to serialize worker configuration (" + format + ")", e);
        }
    }

    private ObjectMapper mapperFor(ConfigurationFormat format) {
        return format == ConfigurationFormat.YAML ? yamlMapper : jsonMapper;
    }
}
