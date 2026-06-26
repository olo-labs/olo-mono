package org.olo.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.source.FileConfigurationSource;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerBootstrapTest {

    @AfterEach
    void tearDown() {
        WorkerBootstrap.shutdown();
    }

    @Test
    void loadsConfigurationThenWorkflowRegistry() {
        Path config = Paths.get("../olo-worker-configuration/samples/worker-config.json")
                .toAbsolutePath().normalize();
        WorkerConfigurationProvider.configure(new FileConfigurationSource(config));

        WorkerRuntimeContext context = WorkerBootstrap.start();

        assertThat(context.settings().id()).isEqualTo("default-worker");
        assertThat(context.workflowRegistry().findById("agent")).isPresent();
        assertThat(context.workflowRegistry().getWorkflows()).hasSize(12);
    }

    @Test
    void cachesContextUntilRefresh() {
        Path config = Paths.get("../olo-worker-configuration/samples/worker-config.json")
                .toAbsolutePath().normalize();
        WorkerConfigurationProvider.configure(new FileConfigurationSource(config));

        WorkerRuntimeContext first = WorkerBootstrap.start();
        assertThat(WorkerBootstrap.start()).isSameAs(first);

        WorkerRuntimeContext refreshed = WorkerBootstrap.start(true);
        assertThat(refreshed).isNotSameAs(first);
        assertThat(refreshed.workflowRegistry().findById("planner")).isPresent();
    }
}
