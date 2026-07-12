/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.bootstrap.registry;

import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory index of workflow definitions loaded from an {@code olo-configuration} folder.
 * <p>
 * Definitions are indexed by {@code id}, {@code id + version}, and {@code queue}. When multiple
 * versions share the same {@code id} or {@code queue}, the version marked {@code isDefault} is
 * exposed through {@link #findById(String)} and {@link #findByQueue(String)}; otherwise the
 * highest version is used. Explicit version lookup uses {@link #findByIdAndVersion(String, String)}
 * and falls back to the default workspace when the requested version is unavailable.
 */
public final class WorkflowDefinitionRegistry {

    private final Path scanFolder;
    private final List<CachedWorkflowDefinition> workflows;
    private final Map<String, WorkflowDefinition> byId;
    private final Map<String, WorkflowDefinition> byIdAndVersion;
    private final Map<String, WorkflowDefinition> defaultById;
    private final Map<String, WorkflowDefinition> byQueue;

    private WorkflowDefinitionRegistry(
            Path scanFolder,
            List<CachedWorkflowDefinition> workflows,
            Map<String, WorkflowDefinition> byId,
            Map<String, WorkflowDefinition> byIdAndVersion,
            Map<String, WorkflowDefinition> defaultById,
            Map<String, WorkflowDefinition> byQueue) {
        this.scanFolder = Objects.requireNonNull(scanFolder, "scanFolder");
        this.workflows = List.copyOf(workflows);
        this.byId = Collections.unmodifiableMap(byId);
        this.byIdAndVersion = Collections.unmodifiableMap(byIdAndVersion);
        this.defaultById = Collections.unmodifiableMap(defaultById);
        this.byQueue = Collections.unmodifiableMap(byQueue);
    }

    public static WorkflowDefinitionRegistry of(Path scanFolder, List<CachedWorkflowDefinition> workflows) {
        Map<String, WorkflowDefinition> byIdAndVersion = new LinkedHashMap<>();
        Map<String, WorkflowDefinition> defaultById = new LinkedHashMap<>();
        Map<String, List<WorkflowDefinition>> definitionsById = new LinkedHashMap<>();
        Map<String, List<WorkflowDefinition>> definitionsByQueue = new LinkedHashMap<>();

        for (CachedWorkflowDefinition cached : workflows) {
            WorkflowDefinition definition = cached.getDefinition();
            if (definition.getId() == null || definition.getId().isBlank()) {
                continue;
            }
            WorkflowDefinitionKey key = WorkflowDefinitionKey.from(definition);
            byIdAndVersion.put(key.compositeKey(), definition);
            definitionsById.computeIfAbsent(definition.getId(), ignored -> new ArrayList<>()).add(definition);
            if (Boolean.TRUE.equals(definition.isDefault())) {
                defaultById.put(definition.getId(), definition);
            }
            String queue = definition.getQueue();
            if (queue != null && !queue.isBlank()) {
                definitionsByQueue.computeIfAbsent(queue, ignored -> new ArrayList<>()).add(definition);
            }
        }

        Map<String, WorkflowDefinition> byId = definitionsById.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> WorkflowDefinitionSelection.selectPrimary(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, WorkflowDefinition> byQueue = definitionsByQueue.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> WorkflowDefinitionSelection.selectPrimary(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));

        return new WorkflowDefinitionRegistry(
                scanFolder, workflows, byId, byIdAndVersion, defaultById, byQueue);
    }

    public Path getScanFolder() {
        return scanFolder;
    }

    public List<CachedWorkflowDefinition> getWorkflows() {
        return workflows;
    }

    /**
     * Primary workflow definition per {@code id} ({@code isDefault} version, else highest version).
     */
    public Map<String, WorkflowDefinition> getWorkflowsById() {
        return byId;
    }

    /**
     * All loaded workflow definitions keyed by {@code id@version}.
     */
    public Map<String, WorkflowDefinition> getWorkflowsByIdAndVersion() {
        return byIdAndVersion;
    }

    /**
     * Workflow definitions explicitly marked {@code isDefault} for each {@code id}.
     */
    public Map<String, WorkflowDefinition> getDefaultWorkflowsById() {
        return defaultById;
    }

    public Map<String, WorkflowDefinition> getWorkflowsByQueue() {
        return byQueue;
    }

    /**
     * Resolves the primary definition for {@code id}.
     */
    public Optional<WorkflowDefinition> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Resolves the default workspace for {@code id} when explicitly marked {@code isDefault}.
     */
    public Optional<WorkflowDefinition> findDefaultById(String id) {
        return Optional.ofNullable(defaultById.get(id));
    }

    /**
     * Resolves an exact {@code id + version} artifact, falling back to the default workspace for
     * {@code id} when the requested version was not loaded.
     */
    public Optional<WorkflowDefinition> findByIdAndVersion(String id, String version) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        WorkflowDefinitionKey key = WorkflowDefinitionKey.of(id, version);
        WorkflowDefinition exact = byIdAndVersion.get(key.compositeKey());
        if (exact != null) {
            return Optional.of(exact);
        }
        return findDefaultById(id).or(() -> findById(id));
    }

    /**
     * Resolves the primary workflow definition registered for {@code queue}.
     */
    public Optional<WorkflowDefinition> findByQueue(String queue) {
        return Optional.ofNullable(byQueue.get(queue));
    }

    /**
     * Resolves a workflow for execution when multiple definitions may share the same Temporal queue.
     * Prefers {@code workflowId} when present; otherwise falls back to {@code queue}.
     */
    public Optional<WorkflowDefinition> resolve(String queue, String workflowId) {
        if (workflowId != null && !workflowId.isBlank()) {
            Optional<WorkflowDefinition> byId = findById(workflowId.trim());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (queue != null && !queue.isBlank()) {
            return findByQueue(queue.trim());
        }
        return Optional.empty();
    }

    /**
     * Temporal workflow type for {@code queue}, taken from the primary workflow definition JSON
     * ({@code workflowType} field). Falls back to {@code olo} when unset.
     */
    public String resolveWorkflowTypeForQueue(String queue) {
        if (queue == null || queue.isBlank()) {
            return "olo";
        }
        return findByQueue(queue.trim())
                .map(WorkflowDefinition::getWorkflowType)
                .filter(type -> type != null && !type.isBlank())
                .map(String::trim)
                .orElse("olo");
    }
}
