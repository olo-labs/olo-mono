/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Folder of active workflow JSON under {@code olo-configuration/current-active}.
 * Scans recursively; presets may live in subfolders such as {@code agents/agent.json}.
 */
@ConfigurationProperties(prefix = "olo.configuration")
public class OloConfigurationProperties {

    /** Absolute or workspace-relative path to the active configuration folder. */
    private String directory = "../../olo-mono/olo-definition/olo-configuration/current-active";

    /** Optional override for dynamic subgraph injection logs ({@code olo-configuration/log}). */
    private String logDirectory = "";

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public Path resolvedDirectory() {
        return Paths.get(directory).toAbsolutePath().normalize();
    }

    /**
     * Parent of the active folder (e.g. {@code olo-configuration} when active is {@code current-active}).
     */
    public Path resolvedCatalogRoot() {
        Path active = resolvedDirectory();
        Path parent = active.getParent();
        if (parent == null) {
            throw new IllegalStateException("Configuration catalog root not found for: " + active);
        }
        return parent.toAbsolutePath().normalize();
    }

    public Path resolvedLogDirectory() {
        if (logDirectory != null && !logDirectory.isBlank()) {
            return Paths.get(logDirectory).toAbsolutePath().normalize();
        }
        Path configRoot = resolvedDirectory();
        Path nested = configRoot.resolve("log");
        if (Files.isDirectory(nested)) {
            return nested;
        }
        Path sibling = configRoot.getParent() != null
                ? configRoot.getParent().resolve("log")
                : nested;
        if (Files.isDirectory(sibling)) {
            return sibling;
        }
        return sibling.toAbsolutePath().normalize();
    }
}
