/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.olo.core.tool.observability.ObservabilitySupport;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilitySupportTest {

    @Test
    void resolvesDemoDataFromMonorepoRoot() {
        Path toolsDirectory = findToolsDirectory();
        assertThat(toolsDirectory).isNotNull();

        Path resolved = ObservabilitySupport.resolveDataFolderPath("demo-data/logs");

        assertThat(resolved).isEqualTo(toolsDirectory.resolve("demo-data").resolve("logs").normalize());
        assertThat(Files.isDirectory(resolved)).isTrue();
    }

    @Test
    void resolvesDemoDataWhenWorkerRunsFromOloWorkerDirectory() {
        Path toolsDirectory = findToolsDirectory();
        Path workerDirectory = toolsDirectory.getParent().getParent().resolve("olo-worker");
        if (!Files.isDirectory(workerDirectory)) {
            return;
        }

        String previousUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", workerDirectory.toString());

            Path resolved = ObservabilitySupport.resolveDataFolderPath("demo-data/logs");

            assertThat(resolved).isEqualTo(toolsDirectory.resolve("demo-data").resolve("logs").normalize());
            assertThat(Files.isDirectory(resolved)).isTrue();
        } finally {
            System.setProperty("user.dir", previousUserDir);
        }
    }

    @Test
    void honorsDemoDataRootOverride() {
        Path toolsDirectory = findToolsDirectory();
        assertThat(toolsDirectory).isNotNull();

        String previous = System.getProperty(ObservabilitySupport.DEMO_DATA_ROOT_ENV);
        try {
            System.setProperty(ObservabilitySupport.DEMO_DATA_ROOT_ENV, toolsDirectory.toString());

            Path resolved = ObservabilitySupport.resolveDataFolderPath("demo-data/cpu");

            assertThat(resolved).isEqualTo(toolsDirectory.resolve("demo-data").resolve("cpu").normalize());
            assertThat(Files.isDirectory(resolved)).isTrue();
        } finally {
            if (previous == null) {
                System.clearProperty(ObservabilitySupport.DEMO_DATA_ROOT_ENV);
            } else {
                System.setProperty(ObservabilitySupport.DEMO_DATA_ROOT_ENV, previous);
            }
        }
    }

    private static Path findToolsDirectory() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 12 && current != null; depth++) {
            Path coreTools = current.resolve("olo-core").resolve("tools");
            if (Files.isDirectory(coreTools.resolve("demo-data"))) {
                return coreTools;
            }
            current = current.getParent();
        }
        return null;
    }
}
