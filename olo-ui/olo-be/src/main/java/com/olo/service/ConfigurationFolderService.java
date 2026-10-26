/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.config.OloConfigurationProperties;
import com.olo.dto.ConfigurationFolderActivateResponse;
import com.olo.dto.ConfigurationFolderListResponse;
import com.olo.dto.ConfigurationFolderListResponse.ConfigurationFolderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lists scenario folders under the configuration catalog and activates one by copying it into
 * {@code current-active}. Excludes {@code current-active} and {@code log} (runtime injection logs).
 */
@Service
public class ConfigurationFolderService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationFolderService.class);
    private static final String ACTIVE_FOLDER_NAME = "current-active";
    private static final String LOG_FOLDER_NAME = "log";
    private static final String ACTIVE_MARKER_FILE = ".olo-active-source";
    private static final Set<String> EXCLUDED_CATALOG_FOLDERS = Set.of(ACTIVE_FOLDER_NAME, LOG_FOLDER_NAME);

    private final OloConfigurationProperties properties;
    private final SystemRefreshService systemRefreshService;

    public ConfigurationFolderService(
            OloConfigurationProperties properties,
            SystemRefreshService systemRefreshService) {
        this.properties = properties;
        this.systemRefreshService = systemRefreshService;
    }

    public ConfigurationFolderListResponse listFolders() throws IOException {
        Path catalogRoot = catalogRoot();
        Path activeDirectory = activeDirectory();
        String activeFolderId = readActiveFolderId(activeDirectory).orElse("");

        List<ConfigurationFolderSummary> folders = new ArrayList<>();
        try (Stream<Path> children = Files.list(catalogRoot)) {
            children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !EXCLUDED_CATALOG_FOLDERS.contains(name))
                    .sorted(Comparator.naturalOrder())
                    .forEach(name -> folders.add(new ConfigurationFolderSummary(name, name.equals(activeFolderId))));
        }

        return new ConfigurationFolderListResponse(
                catalogRoot.toString(),
                activeDirectory.toString(),
                activeFolderId,
                folders);
    }

    public ConfigurationFolderActivateResponse activateFolder(String folderId) throws IOException {
        String safeFolderId = validateFolderId(folderId);
        Path catalogRoot = catalogRoot();
        Path source = catalogRoot.resolve(safeFolderId).normalize();
        if (!source.startsWith(catalogRoot) || !Files.isDirectory(source)) {
            throw new IllegalArgumentException("configuration folder not found: " + safeFolderId);
        }

        Path activeDirectory = activeDirectory();
        Files.createDirectories(activeDirectory);
        log.info("Activating configuration folder {} -> {}", source, activeDirectory);
        clearDirectory(activeDirectory);
        copyDirectory(source, activeDirectory);
        writeActiveMarker(activeDirectory, safeFolderId);

        var refresh = systemRefreshService.refreshAll();
        return new ConfigurationFolderActivateResponse(
                safeFolderId,
                activeDirectory.toString(),
                refresh);
    }

    public Path catalogRoot() {
        Path root = properties.resolvedCatalogRoot();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Configuration catalog root not found: " + root);
        }
        return root;
    }

    public Path activeDirectory() {
        Path active = properties.resolvedDirectory();
        if (!Files.isDirectory(active)) {
            throw new IllegalStateException("Active configuration directory not found: " + active);
        }
        return active;
    }

    static String validateFolderId(String folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("folder id is required");
        }
        String trimmed = folderId.trim();
        if (trimmed.isEmpty() || EXCLUDED_CATALOG_FOLDERS.contains(trimmed)) {
            throw new IllegalArgumentException("invalid folder id");
        }
        if (!trimmed.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid folder id");
        }
        return trimmed;
    }

    private static Optional<String> readActiveFolderId(Path activeDirectory) throws IOException {
        Path marker = activeDirectory.resolve(ACTIVE_MARKER_FILE);
        if (!Files.isRegularFile(marker)) {
            return Optional.empty();
        }
        String value = Files.readString(marker, StandardCharsets.UTF_8).trim();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static void writeActiveMarker(Path activeDirectory, String folderId) throws IOException {
        Files.writeString(
                activeDirectory.resolve(ACTIVE_MARKER_FILE),
                folderId,
                StandardCharsets.UTF_8);
    }

    private static void clearDirectory(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path src : walk.toList()) {
                Path relative = source.relativize(src);
                if (relative.toString().replace('\\', '/').equals(ACTIVE_MARKER_FILE)) {
                    continue;
                }
                Path dest = target.resolve(relative);
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
