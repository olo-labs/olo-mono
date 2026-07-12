/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * Non-deterministic kernel work executed outside the Temporal workflow sandbox.
 */
@ActivityInterface
public interface OloKernelActivities {

    @ActivityMethod
    KernelExecutionSnapshot buildContextAndNotifyUi(String queue, WorkflowInput input);

    @ActivityMethod
    KernelExecutionSnapshot executeTraversalStep(KernelExecutionSnapshot snapshot);

    @ActivityMethod
    String reportWorkflowResult(KernelExecutionSnapshot snapshot);
}
