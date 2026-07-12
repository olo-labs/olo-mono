/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.bootstrap.registry;

import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Objects;

/**
 * Composite identity for a workflow definition artifact ({@code id} + {@code version}).
 */
public record WorkflowDefinitionKey(String id, String version) {

    private static final String SEPARATOR = "@";

    public WorkflowDefinitionKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
    }

    public static WorkflowDefinitionKey of(String id, String version) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("workflow id is required");
        }
        return new WorkflowDefinitionKey(id.trim(), normalizeVersion(version));
    }

    public static WorkflowDefinitionKey from(WorkflowDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return of(definition.getId(), definition.getVersion());
    }

    public String compositeKey() {
        return id + SEPARATOR + version;
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "";
        }
        return version.trim();
    }
}
