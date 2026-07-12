/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.variables;

import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable workflow variable values for a single execution, initialized from graph variable definitions.
 */
public final class WorkflowRuntimeVariables {

    private final Map<String, Object> values;

    private WorkflowRuntimeVariables(Map<String, Object> values) {
        this.values = values;
    }

    public static WorkflowRuntimeVariables fromDefinition(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        Map<String, Object> initial = new LinkedHashMap<>();
        for (VariableDefinition variable : graph.getVariables()) {
            if (variable == null || variable.getName() == null || variable.getName().isBlank()) {
                continue;
            }
            initial.put(variable.getName(), variable.getDefaultValue());
        }
        return new WorkflowRuntimeVariables(initial);
    }

    public static WorkflowRuntimeVariables fromMap(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        return new WorkflowRuntimeVariables(new LinkedHashMap<>(values));
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    public Object get(String name) {
        return values.get(name);
    }

    public void set(String name, Object value) {
        values.put(name, value);
    }

    public String getString(String name) {
        Object value = values.get(name);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    /**
     * Snapshot of all workflow variable names and their current values (including {@code null} defaults).
     */
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
