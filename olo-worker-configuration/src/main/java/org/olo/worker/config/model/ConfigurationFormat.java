/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.model;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Serialization format for a workflow definition artifact.
 */
public enum ConfigurationFormat {
    JSON,
    YAML;

    /**
     * Detects format from a file name extension ({@code .json}, {@code .yaml}, {@code .yml}).
     */
    public static Optional<ConfigurationFormat> fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return Optional.of(JSON);
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return Optional.of(YAML);
        }
        return Optional.empty();
    }

    /**
     * Detects format from a path's file name.
     */
    public static Optional<ConfigurationFormat> fromPath(Path path) {
        if (path == null) {
            return Optional.empty();
        }
        Path fileName = path.getFileName();
        return fileName == null ? Optional.empty() : fromFileName(fileName.toString());
    }
}
