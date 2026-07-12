/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.releasereadinessreview;

import java.nio.file.Files;
import java.nio.file.Path;

final class ReleaseReadinessReviewPaths {

    private static final String RELATIVE = "olo-configuration/release-readiness-review";
    private static final String PROPERTY = "olo.releaseReadinessReview.configuration.dir";

    private ReleaseReadinessReviewPaths() {
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
