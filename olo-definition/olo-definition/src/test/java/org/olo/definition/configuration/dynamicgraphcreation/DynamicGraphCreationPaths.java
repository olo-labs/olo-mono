/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.dynamicgraphcreation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the on-disk {@code olo-configuration/dynamic-graph-creation/} root. */
final class DynamicGraphCreationPaths {

    private DynamicGraphCreationPaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.dynamicGraphCreation.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {"olo-configuration/dynamic-graph-creation", "../olo-configuration/dynamic-graph-creation"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration/dynamic-graph-creation").toAbsolutePath();
    }
}
