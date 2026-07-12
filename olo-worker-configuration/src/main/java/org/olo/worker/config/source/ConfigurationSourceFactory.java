/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.source;

import org.olo.worker.config.bootstrap.WorkerConfigurationBootstrap;
import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.ConfigurationSourceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Resolves which {@link ConfigurationSource} to use. This is the only place that reads bootstrap
 * environment variables for locating configuration storage.
 */
public final class ConfigurationSourceFactory {

    private ConfigurationSourceFactory() {
    }

    /**
     * Creates a configuration source from bootstrap environment variables / system properties.
     */
    public static ConfigurationSource fromBootstrap() {
        return forSourceType(resolveSourceType(), resolveFilePath());
    }

    public static ConfigurationSource forFile(Path path) {
        return new FileConfigurationSource(path);
    }

    public static ConfigurationSource forSourceType(ConfigurationSourceType sourceType, Path filePath) {
        return switch (sourceType) {
            case FILE -> new FileConfigurationSource(filePath);
            case DATABASE, REDIS, GITHUB -> throw new WorkerConfigurationException(
                    "configuration source not implemented yet: " + sourceType
                            + ". Add a " + sourceType.name().toLowerCase(Locale.ROOT)
                            + " ConfigurationSource implementation.");
        };
    }

    public static ConfigurationSourceType resolveSourceType() {
        String raw = firstNonBlank(
                System.getenv(WorkerConfigurationBootstrap.ENV_SOURCE),
                System.getProperty(WorkerConfigurationBootstrap.PROP_SOURCE),
                WorkerConfigurationBootstrap.DEFAULT_SOURCE);
        try {
            return ConfigurationSourceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new WorkerConfigurationException("unknown configuration source: " + raw);
        }
    }

    public static Path resolveFilePath() {
        String raw = firstNonBlank(
                System.getenv(WorkerConfigurationBootstrap.ENV_FILE_PATH),
                System.getProperty(WorkerConfigurationBootstrap.PROP_FILE_PATH),
                WorkerConfigurationBootstrap.DEFAULT_FILE_PATH);
        Path configured = Path.of(raw.trim()).normalize();
        if (Files.exists(configured)) {
            return configured;
        }
        for (Path candidate : monorepoFallbackPaths()) {
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
        }
        return configured;
    }

    private static List<Path> monorepoFallbackPaths() {
        return List.of(
                Path.of("../olo-worker-configuration/samples/worker-config.yaml"),
                Path.of("../olo-worker-configuration/samples/worker-config.json"),
                Path.of("olo-worker-configuration/samples/worker-config.yaml"),
                Path.of("olo-worker-configuration/samples/worker-config.json"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
