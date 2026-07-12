/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.source.FileConfigurationSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerBootstrapTest {

    private static final Path TEST_CONFIG = Paths.get("../olo-worker-configuration/samples/worker-config-test.json")
            .toAbsolutePath()
            .normalize();

    @AfterEach
    void tearDown() {
        WorkerBootstrap.shutdown();
    }

    @Test
    void loadsConfigurationThenWorkflowRegistry() {
        assumeTestConfigExists();
        WorkerConfigurationProvider.configure(new FileConfigurationSource(TEST_CONFIG));

        WorkerRuntimeContext context = WorkerBootstrap.start();

        assertThat(context.settings().id()).isEqualTo("default-worker");
        assertThat(context.workflowRegistry().findById("agent")).isPresent();
        assertThat(context.workflowRegistry().getWorkflows()).hasSize(12);
    }

    @Test
    void cachesContextUntilRefresh() {
        assumeTestConfigExists();
        WorkerConfigurationProvider.configure(new FileConfigurationSource(TEST_CONFIG));

        WorkerRuntimeContext first = WorkerBootstrap.start();
        assertThat(WorkerBootstrap.start()).isSameAs(first);

        WorkerRuntimeContext refreshed = WorkerBootstrap.start(true);
        assertThat(refreshed).isNotSameAs(first);
        assertThat(refreshed.workflowRegistry().findById("planner")).isPresent();
    }

    private static void assumeTestConfigExists() {
        if (!Files.exists(TEST_CONFIG)) {
            throw new org.opentest4j.TestAbortedException("worker test config not found at " + TEST_CONFIG);
        }
    }
}
