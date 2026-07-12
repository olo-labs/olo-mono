/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.runtime.RuntimeDelegationDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;

/**
 * Applies default runtime execution model and migrates legacy orchestration blocks before build.
 */
final class WorkflowDefinitionBuilderRuntime {

    private WorkflowDefinitionBuilderRuntime() {
    }

    static void normalize(WorkflowDefinitionBuilderBase builder) {
        ensureRuntime(builder);
        mergeLegacyOrchestrationIntoRuntime(builder);
    }

    private static void ensureRuntime(WorkflowDefinitionBuilderBase builder) {
        if (builder.runtime == null) {
            builder.runtime = WorkflowRuntimeDefinition.builder()
                    .executionModel(ExecutionModel.INLINE)
                    .build();
            return;
        }
        if (builder.runtime.getExecutionModel() == null) {
            builder.runtime = WorkflowRuntimeDefinition.builder()
                    .contractVersion(builder.runtime.getContractVersion())
                    .executionModel(ExecutionModel.INLINE)
                    .capabilities(builder.runtime.getCapabilities())
                    .defaultTimeout(builder.runtime.getDefaultTimeout())
                    .delegation(builder.runtime.getDelegation())
                    .build();
        }
    }

    private static void mergeLegacyOrchestrationIntoRuntime(WorkflowDefinitionBuilderBase builder) {
        if (builder.legacyOrchestration == null) {
            return;
        }
        RuntimeDelegationDefinition delegation =
                RuntimeDelegationDefinition.fromLegacy(builder.legacyOrchestration);
        if (builder.runtime == null) {
            builder.runtime = WorkflowRuntimeDefinition.builder().delegation(delegation).build();
            return;
        }
        if (builder.runtime.getDelegation() == null) {
            builder.runtime = WorkflowRuntimeDefinition.builder()
                    .contractVersion(builder.runtime.getContractVersion())
                    .executionModel(builder.runtime.getExecutionModel())
                    .capabilities(builder.runtime.getCapabilities())
                    .defaultTimeout(builder.runtime.getDefaultTimeout())
                    .delegation(delegation)
                    .build();
        }
    }
}
