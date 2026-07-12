/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.capacityplanning;

import java.nio.file.Files;
import java.nio.file.Path;

final class CapacityPlanningPaths {

    private static final String RELATIVE = "olo-configuration/capacity-planning";
    private static final String PROPERTY = "olo.capacityPlanning.configuration.dir";

    private CapacityPlanningPaths() {
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
