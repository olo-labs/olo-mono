/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.worker.config.WorkerSettings;

import java.util.Objects;

/**
 * Holds the in-memory worker configuration and workflow definition registry after bootstrap.
 */
public final class WorkerRuntimeContext {

    private final WorkerSettings settings;
    private final WorkflowDefinitionRegistry workflowRegistry;

    public WorkerRuntimeContext(WorkerSettings settings, WorkflowDefinitionRegistry workflowRegistry) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.workflowRegistry = Objects.requireNonNull(workflowRegistry, "workflowRegistry");
    }

    public WorkerSettings settings() {
        return settings;
    }

    public WorkflowDefinitionRegistry workflowRegistry() {
        return workflowRegistry;
    }
}
