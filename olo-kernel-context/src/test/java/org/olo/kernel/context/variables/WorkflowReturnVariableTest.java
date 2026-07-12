/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.variables;

import org.junit.jupiter.api.Test;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.exception.KernelContextException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowReturnVariableTest {

    @Test
    void readsConfiguredNameFromWorkflowMetadata() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .metadata(Map.of(WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue"))
                .variables(List.of(variable("OtherValue")))
                .build();

        assertThat(WorkflowReturnVariable.readConfiguredName(graph)).isEqualTo("ReturnValue");
        assertThat(WorkflowReturnVariable.resolveName(graph)).isEqualTo("ReturnValue");
    }

    @Test
    void prefersWorkflowMetadataReturnVariable() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .metadata(Map.of(WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue"))
                .variables(List.of(
                        variable("ReturnValue"),
                        variable("OtherValue")))
                .build();

        assertThat(WorkflowReturnVariable.resolveName(graph)).isEqualTo("ReturnValue");
    }

    @Test
    void usesSingleVariableMarkedWithReturnRole() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .variables(List.of(
                        variable("OtherValue"),
                        variableWithReturnRole("ReturnValue")))
                .build();

        assertThat(WorkflowReturnVariable.resolveName(graph)).isEqualTo("ReturnValue");
    }

    @Test
    void failsWhenMultipleVariablesMarkedWithReturnRole() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .variables(List.of(
                        variableWithReturnRole("First"),
                        variableWithReturnRole("Second")))
                .build();

        assertThatThrownBy(() -> WorkflowReturnVariable.resolveName(graph))
                .isInstanceOf(KernelContextException.class)
                .hasMessageContaining("multiple variables marked as return");
    }

    @Test
    void fallsBackToDefaultReturnValueName() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("test")
                .name("Test")
                .queue("test")
                .variables(List.of(variable("ReturnValue"), variable("OtherValue")))
                .build();

        assertThat(WorkflowReturnVariable.resolveName(graph)).isEqualTo("ReturnValue");
    }

    private static VariableDefinition variable(String name) {
        return VariableDefinition.builder().name(name).type("string").build();
    }

    private static VariableDefinition variableWithReturnRole(String name) {
        return VariableDefinition.builder()
                .name(name)
                .type("string")
                .metadata(Map.of(WorkflowReturnVariable.VARIABLE_ROLE_METADATA_KEY, "return"))
                .build();
    }
}
