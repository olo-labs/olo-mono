/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.logrcaanalysis;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the on-disk {@code olo-configuration/log-rca-analysis/} root. */
final class LogRcaAnalysisPaths {

    private LogRcaAnalysisPaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.logRcaAnalysis.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {"olo-configuration/log-rca-analysis", "../olo-configuration/log-rca-analysis"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration/log-rca-analysis").toAbsolutePath();
    }
}
