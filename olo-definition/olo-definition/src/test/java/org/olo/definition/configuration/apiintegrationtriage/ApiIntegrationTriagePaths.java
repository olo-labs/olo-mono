/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.apiintegrationtriage;

import java.nio.file.Files;
import java.nio.file.Path;

final class ApiIntegrationTriagePaths {

    private static final String RELATIVE = "olo-configuration/api-integration-triage";
    private static final String PROPERTY = "olo.apiIntegrationTriage.configuration.dir";

    private ApiIntegrationTriagePaths() {
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
