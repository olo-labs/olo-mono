/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.input;

import org.junit.jupiter.api.Test;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.WorkflowReturnOutput;
import org.olo.kernel.context.variables.WorkflowReturnVariable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowReturnResolverOutputTest {

    @Test
    void resolvesConfiguredReturnOutputKey() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("writer-return")
                .metadata(Map.of(
                        WorkflowReturnOutput.WORKFLOW_METADATA_KEY, "writer",
                        WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue"))
                .variables(List.of(VariableDefinition.builder().name("ReturnValue").type("string").build()))
                .build();
        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("writer-return", new WorkflowInput("1.0", List.of(), null, null, null, null), graph));

        context.getOutputs().put("planner", new ExecutionOutput("planner", "PLANNER", "plan", null, Map.of()));
        context.getOutputs().put("writer", new ExecutionOutput("writer", "AGENT", "final answer", null, Map.of()));

        assertThat(WorkflowReturnResolver.resolve(context)).isEqualTo("final answer");
    }

    @Test
    void fallsBackToLastExecutionOutputWhenNoReturnKeysConfigured() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("last")
                .variables(List.of(
                        VariableDefinition.builder().name("message").type("string").build(),
                        VariableDefinition.builder().name("ReturnValue").type("string").build()))
                .build();
        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("last", new WorkflowInput("1.0", List.of(), null, null, null, null), graph));

        context.getOutputs().put("planner", new ExecutionOutput("planner", "PLANNER", "plan", null, Map.of()));
        context.getOutputs().put("writer", new ExecutionOutput("writer", "AGENT", "last wins", null, Map.of()));

        assertThat(WorkflowReturnResolver.resolve(context)).isEqualTo("last wins");
    }
}
