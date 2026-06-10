package org.olo.bootstrap.registry;

import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory index of workflow definitions loaded from an {@code olo-configuration} folder.
 */
public final class WorkflowDefinitionRegistry {

    private final Path scanFolder;
    private final List<CachedWorkflowDefinition> workflows;
    private final Map<String, WorkflowDefinition> byId;
    private final Map<String, WorkflowDefinition> byQueue;

    private WorkflowDefinitionRegistry(
            Path scanFolder,
            List<CachedWorkflowDefinition> workflows,
            Map<String, WorkflowDefinition> byId,
            Map<String, WorkflowDefinition> byQueue) {
        this.scanFolder = Objects.requireNonNull(scanFolder, "scanFolder");
        this.workflows = List.copyOf(workflows);
        this.byId = Collections.unmodifiableMap(byId);
        this.byQueue = Collections.unmodifiableMap(byQueue);
    }

    public static WorkflowDefinitionRegistry of(Path scanFolder, List<CachedWorkflowDefinition> workflows) {
        Map<String, WorkflowDefinition> byId = new LinkedHashMap<>();
        Map<String, WorkflowDefinition> byQueue = new LinkedHashMap<>();
        for (CachedWorkflowDefinition cached : workflows) {
            WorkflowDefinition definition = cached.getDefinition();
            if (definition.getId() != null && !definition.getId().isBlank()) {
                byId.put(definition.getId(), definition);
            }
            if (definition.getQueue() != null && !definition.getQueue().isBlank()) {
                byQueue.put(definition.getQueue(), definition);
            }
        }
        return new WorkflowDefinitionRegistry(scanFolder, workflows, byId, byQueue);
    }

    public Path getScanFolder() {
        return scanFolder;
    }

    public List<CachedWorkflowDefinition> getWorkflows() {
        return workflows;
    }

    public Map<String, WorkflowDefinition> getWorkflowsById() {
        return byId;
    }

    public Map<String, WorkflowDefinition> getWorkflowsByQueue() {
        return byQueue;
    }

    public Optional<WorkflowDefinition> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<WorkflowDefinition> findByQueue(String queue) {
        return Optional.ofNullable(byQueue.get(queue));
    }
}
