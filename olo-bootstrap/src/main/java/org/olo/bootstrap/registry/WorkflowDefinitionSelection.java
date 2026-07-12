/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.bootstrap.registry;

import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Chooses the primary workflow artifact for an {@code id} or {@code queue} among multiple versions.
 */
final class WorkflowDefinitionSelection {

    private WorkflowDefinitionSelection() {
    }

    static WorkflowDefinition selectPrimary(List<WorkflowDefinition> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        for (WorkflowDefinition candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.isDefault())) {
                return candidate;
            }
        }
        return candidates.stream()
                .max(Comparator.comparing(
                        WorkflowDefinition::getVersion,
                        WorkflowVersionOrder.comparator()))
                .orElseThrow();
    }
}
