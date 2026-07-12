/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.performancetriage;

import java.nio.file.Files;
import java.nio.file.Path;

final class PerformanceTriagePaths {

    private static final String RELATIVE = "olo-configuration/performance-triage";
    private static final String PROPERTY = "olo.performanceTriage.configuration.dir";

    private PerformanceTriagePaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate : new String[] {RELATIVE, "../" + RELATIVE}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of(RELATIVE).toAbsolutePath();
    }
}
