/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.common.converter.EncodedValues;
import org.junit.jupiter.api.Test;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

import static org.assertj.core.api.Assertions.assertThat;

class OloKernelDynamicActivityRoutingTest {

    @Test
    void routesTraversalStepByOperationString() {
        KernelExecutionSnapshot snapshot = minimalSnapshot();
        EncodedValues args = new EncodedValues(snapshot, KernelActivityOperations.STEP);

        assertThat(args.getSize()).isEqualTo(2);
        assertThat(args.get(1, String.class)).isEqualTo(KernelActivityOperations.STEP);
        assertThat(args.get(0, KernelExecutionSnapshot.class).getNextActivityName()).isEqualTo("start:Start");
    }

    @Test
    void routesBuildContextWithQueueAndWorkflowInput() {
        WorkflowInput input = WorkflowInput.fromJson("{\"message\":\"hello\"}");
        EncodedValues args = new EncodedValues("dynamic-graph-creation", input);

        assertThat(args.getSize()).isEqualTo(2);
        assertThat(args.get(0, String.class)).isEqualTo("dynamic-graph-creation");
        assertThat(args.get(1, WorkflowInput.class)).isNotNull();
    }

    @Test
    void routesReportWithSingleSnapshotArgument() {
        KernelExecutionSnapshot snapshot = minimalSnapshot();
        EncodedValues args = new EncodedValues(snapshot);

        assertThat(args.getSize()).isEqualTo(1);
        assertThat(args.get(0, KernelExecutionSnapshot.class).getQueue()).isEqualTo("dynamic-graph-creation");
    }

    private static KernelExecutionSnapshot minimalSnapshot() {
        WorkflowInput input = WorkflowInput.fromJson("{\"message\":\"hello\"}");
        return new KernelExecutionSnapshot(
                "dynamic-graph-creation",
                input,
                "{\"id\":\"dynamic-graph-creation\",\"nodes\":[],\"edges\":[]}",
                java.util.Map.of(),
                java.util.Map.of(),
                "start",
                0,
                KernelExecutionSnapshot.Status.RUNNING,
                null,
                null,
                null,
                true,
                "dynamic-graph-creation:Dynamic Graph Creation",
                "start:Start");
    }
}
