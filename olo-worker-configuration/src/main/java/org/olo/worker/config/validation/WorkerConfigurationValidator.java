package org.olo.worker.config.validation;

import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.CacheSettings;
import org.olo.worker.config.model.InputSettings;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural validation for a loaded {@link WorkerConfiguration}.
 */
public final class WorkerConfigurationValidator {

    private WorkerConfigurationValidator() {
    }

    public static WorkerConfigurationValidationResult validate(WorkerConfiguration configuration) {
        List<String> errors = new ArrayList<>();
        if (configuration == null) {
            return WorkerConfigurationValidationResult.failure(List.of("configuration must not be null"));
        }
        if (isBlank(configuration.getId())) {
            errors.add("worker id is required");
        }
        validateServer(configuration.getServer(), errors);
        validateWorkflowDefinitions(configuration.getWorkflowDefinitions(), errors);
        validateInput(configuration.getInput(), errors);
        validateCache(configuration.getCache(), errors);
        return errors.isEmpty()
                ? WorkerConfigurationValidationResult.success()
                : WorkerConfigurationValidationResult.failure(errors);
    }

    public static void validateOrThrow(WorkerConfiguration configuration) {
        WorkerConfigurationValidationResult result = validate(configuration);
        if (!result.isValid()) {
            throw new WorkerConfigurationException(String.join("; ", result.getErrors()));
        }
    }

    private static void validateServer(ServerSettings server, List<String> errors) {
        if (server == null) {
            errors.add("server settings are required");
            return;
        }
        if (server.getPort() == null) {
            errors.add("server.port is required");
        } else if (server.getPort() < 1 || server.getPort() > 65535) {
            errors.add("server.port must be between 1 and 65535");
        }
        if (server.getHost() != null && server.getHost().isBlank()) {
            errors.add("server.host must not be blank when set");
        }
    }

    private static void validateWorkflowDefinitions(WorkflowDefinitionsSettings settings, List<String> errors) {
        if (settings == null) {
            errors.add("workflowDefinitions settings are required");
            return;
        }
        if (isBlank(settings.getScanFolder())) {
            errors.add("workflowDefinitions.scanFolder is required");
        }
    }

    private static void validateInput(InputSettings input, List<String> errors) {
        if (input == null || input.getMaxLocalMessageSize() == null) {
            return;
        }
        if (input.getMaxLocalMessageSize() < 1) {
            errors.add("input.maxLocalMessageSize must be at least 1");
        }
    }

    private static void validateCache(CacheSettings cache, List<String> errors) {
        if (cache == null || !cache.isEnabled()) {
            return;
        }
        if (isBlank(cache.getHost())) {
            errors.add("cache.host is required when cache.enabled is true");
        }
        if (cache.getPort() != null && (cache.getPort() < 1 || cache.getPort() > 65535)) {
            errors.add("cache.port must be between 1 and 65535");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
