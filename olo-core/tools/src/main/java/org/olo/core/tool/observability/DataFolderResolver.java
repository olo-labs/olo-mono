/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.observability;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves demo-data folder paths from tool configuration, cwd, or environment overrides.
 */
final class DataFolderResolver {

    static final String DEMO_DATA_ROOT_ENV = "OLO_DEMO_DATA_ROOT";

    private DataFolderResolver() {
    }

    static Path resolveDataFolderPath(String configured) {
        Path path = Path.of(configured).normalize();
        if (path.isAbsolute()) {
            return path;
        }
        if (Files.isDirectory(path)) {
            return path.toAbsolutePath().normalize();
        }
        Path toolsDirectory = discoverToolsDirectory();
        if (toolsDirectory != null) {
            Path resolved = toolsDirectory.resolve(path).normalize();
            if (Files.isDirectory(resolved)) {
                return resolved;
            }
        }
        return path;
    }

    /** Walks parent directories looking for a {@code demo-data} folder in known layouts. */
    static Path discoverToolsDirectory() {
        Path override = readDemoDataRootOverride();
        if (override != null) {
            return override;
        }
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 12 && current != null; depth++) {
            if (Files.isDirectory(current.resolve("demo-data"))) {
                return current;
            }
            Path monoTools = current.resolve("olo-mono").resolve("olo-core").resolve("tools");
            if (Files.isDirectory(monoTools.resolve("demo-data"))) {
                return monoTools;
            }
            Path coreTools = current.resolve("olo-core").resolve("tools");
            if (Files.isDirectory(coreTools.resolve("demo-data"))) {
                return coreTools;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Path readDemoDataRootOverride() {
        String configured = System.getenv(DEMO_DATA_ROOT_ENV);
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty(DEMO_DATA_ROOT_ENV);
        }
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return Path.of(configured.trim());
    }
}
