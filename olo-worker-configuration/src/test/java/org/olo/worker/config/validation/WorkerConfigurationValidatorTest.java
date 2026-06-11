package org.olo.worker.config.validation;

import org.junit.jupiter.api.Test;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerConfigurationValidatorTest {

    @Test
    void acceptsValidConfiguration() {
        WorkerConfiguration configuration = validConfiguration();

        assertThat(WorkerConfigurationValidator.validate(configuration).isValid()).isTrue();
    }

    @Test
    void rejectsMissingPort() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().host("127.0.0.1").build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("/defs")
                        .build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("server.port"));
    }

    @Test
    void rejectsInvalidPort() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(70000).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("/defs")
                        .build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("server.port"));
    }

    @Test
    void rejectsMissingScanFolder() {
        WorkerConfiguration configuration = WorkerConfiguration.builder()
                .id("worker")
                .server(ServerSettings.builder().port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder().build())
                .build();

        assertThat(WorkerConfigurationValidator.validate(configuration).getErrors())
                .anyMatch(error -> error.contains("scanFolder"));
    }

    private static WorkerConfiguration validConfiguration() {
        return WorkerConfiguration.builder()
                .id("worker")
                .name("Worker")
                .server(ServerSettings.builder().host("0.0.0.0").port(8080).build())
                .workflowDefinitions(WorkflowDefinitionsSettings.builder()
                        .scanFolder("../olo-definition/olo-configuration/default")
                        .recursive(false)
                        .build())
                .build();
    }
}
