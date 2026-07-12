/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationFormatTest {

    @Test
    void detectsJsonAndYamlExtensions() {
        assertThat(ConfigurationFormat.fromFileName("agent.json")).contains(ConfigurationFormat.JSON);
        assertThat(ConfigurationFormat.fromFileName("workflow.yaml")).contains(ConfigurationFormat.YAML);
        assertThat(ConfigurationFormat.fromFileName("workflow.yml")).contains(ConfigurationFormat.YAML);
        assertThat(ConfigurationFormat.fromFileName("readme.md")).isEmpty();
    }

    @Test
    void detectsFromPath() {
        assertThat(ConfigurationFormat.fromPath(Path.of("dir", "workflow.json")))
                .contains(ConfigurationFormat.JSON);
    }
}
