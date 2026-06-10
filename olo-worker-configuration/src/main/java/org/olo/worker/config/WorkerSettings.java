package org.olo.worker.config;

import org.olo.worker.config.model.CacheSettings;
import org.olo.worker.config.model.InputSettings;
import org.olo.worker.config.model.ServerSettings;
import org.olo.worker.config.model.TemporalSettings;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.model.WorkflowDefinitionsSettings;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Typed read-only view of worker configuration. Prefer this over reading environment variables directly.
 */
public final class WorkerSettings {

    private static final String DEFAULT_SERVER_HOST = "0.0.0.0";

    private final WorkerConfiguration configuration;

    public WorkerSettings(WorkerConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public WorkerConfiguration configuration() {
        return configuration;
    }

    public String id() {
        return configuration.getId();
    }

    public String name() {
        return configuration.getName();
    }

    public String serverHost() {
        ServerSettings server = configuration.getServer();
        if (server == null || server.getHost() == null || server.getHost().isBlank()) {
            return DEFAULT_SERVER_HOST;
        }
        return server.getHost();
    }

    public int serverPort() {
        return configuration.getServer().getPort();
    }

    public Path workflowDefinitionsScanFolder() {
        return Path.of(configuration.getWorkflowDefinitions().getScanFolder());
    }

    /**
     * Resolves {@link #workflowDefinitionsScanFolder()} against {@code baseDirectory} when the path is relative.
     */
    public Path resolvedWorkflowDefinitionsScanFolder(Path baseDirectory) {
        Path folder = workflowDefinitionsScanFolder();
        if (folder.isAbsolute()) {
            return folder.normalize();
        }
        return Objects.requireNonNull(baseDirectory, "baseDirectory").resolve(folder).normalize();
    }

    public boolean workflowDefinitionsRecursive() {
        WorkflowDefinitionsSettings settings = configuration.getWorkflowDefinitions();
        return settings != null && Boolean.TRUE.equals(settings.getRecursive());
    }

    public int maxLocalMessageSize() {
        InputSettings input = configuration.getInput();
        return input == null ? InputSettings.DEFAULT_MAX_LOCAL_MESSAGE_SIZE : input.resolveMaxLocalMessageSize();
    }

    public TemporalSettings temporal() {
        return configuration.getTemporal();
    }

    public CacheSettings cache() {
        return configuration.getCache();
    }

    public Map<String, Object> metadata() {
        return configuration.getMetadata();
    }
}
