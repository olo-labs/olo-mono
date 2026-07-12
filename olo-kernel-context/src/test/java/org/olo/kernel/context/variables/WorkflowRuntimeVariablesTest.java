/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.variables;

import org.junit.jupiter.api.Test;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRuntimeVariablesTest {

    @Test
    void initializesFromVariableDefinitions() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .variables(List.of(
                        VariableDefinition.builder()
                                .name("ReturnValue")
                                .type("string")
                                .defaultValue(null)
                                .build()))
                .build();

        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromDefinition(graph);

        assertThat(variables.has("ReturnValue")).isTrue();
        assertThat(variables.get("ReturnValue")).isNull();
        assertThat(variables.toMap()).containsEntry("ReturnValue", null);
    }
}
