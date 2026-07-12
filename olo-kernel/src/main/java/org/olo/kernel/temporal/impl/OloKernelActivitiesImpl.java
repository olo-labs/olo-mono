/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal.impl;

import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.temporal.OloKernelActivities;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

public final class OloKernelActivitiesImpl implements OloKernelActivities {

    @Override
    public KernelExecutionSnapshot buildContextAndNotifyUi(String queue, WorkflowInput input) {
        return KernelEntryPoint.buildContextAndNotifyUi(queue, input, KernelRuntimeHolder.registry());
    }

    @Override
    public KernelExecutionSnapshot executeTraversalStep(KernelExecutionSnapshot snapshot) {
        return KernelEntryPoint.executeTraversalStep(snapshot);
    }

    @Override
    public String reportWorkflowResult(KernelExecutionSnapshot snapshot) {
        return KernelEntryPoint.reportWorkflowResult(snapshot);
    }
}
