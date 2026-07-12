/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.worker.config.source.ConfigurationSourceFactory;
import org.olo.worker.config.source.FileConfigurationSource;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerConfigurationProviderTest {

    @AfterEach
    void tearDown() {
        WorkerConfigurationProvider.reset();
    }

    @Test
    void loadsThroughProviderApi() {
        WorkerConfigurationProvider.configure(new FileConfigurationSource(
                Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize()));

        WorkerSettings settings = WorkerConfigurationProvider.load();

        assertThat(settings.id()).isEqualTo("default-worker");
        assertThat(settings.serverPort()).isEqualTo(8080);
        assertThat(settings.workflowDefinitionsScanFolder().normalize().toString())
                .contains("olo-configuration")
                .endsWith("default");
        assertThat(settings.maxLocalMessageSize()).isEqualTo(50);
        assertThat(settings.cache().isEnabled()).isTrue();
        assertThat(settings.temporal().getNamespace()).isEqualTo("default");
    }

    @Test
    void cachesConfigurationUntilRefreshRequested() {
        WorkerConfigurationProvider.configure(new FileConfigurationSource(
                Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize()));

        WorkerSettings first = WorkerConfigurationProvider.load();
        assertThat(WorkerConfigurationProvider.load()).isSameAs(first);

        WorkerSettings refreshed = WorkerConfigurationProvider.load(true);
        assertThat(refreshed).isNotSameAs(first);
        assertThat(refreshed.id()).isEqualTo(first.id());
        assertThat(WorkerConfigurationProvider.load()).isSameAs(refreshed);
    }

    @Test
    void exposesConfigurationBaseDirectoryForRelativePaths() {
        var configPath = Paths.get("samples/worker-config.yaml").toAbsolutePath().normalize();
        WorkerConfigurationProvider.configure(new FileConfigurationSource(configPath));

        assertThat(WorkerConfigurationProvider.configurationBaseDirectory())
                .isEqualTo(configPath.getParent());
    }

    @Test
    void factoryResolvesFileSourceFromBootstrap() {
        assertThat(ConfigurationSourceFactory.resolveSourceType().name()).isEqualTo("FILE");
        assertThat(ConfigurationSourceFactory.resolveFilePath().getFileName().toString())
                .isEqualTo("worker-config.yaml");
    }
}
