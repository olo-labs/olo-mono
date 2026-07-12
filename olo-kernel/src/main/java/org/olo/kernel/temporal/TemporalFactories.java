/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import org.olo.kernel.temporal.impl.OloKernelActivitiesImpl;
import org.olo.kernel.temporal.impl.OloKernelDynamicActivity;
import org.olo.kernel.temporal.impl.OloKernelWorkflowImpl;

public final class TemporalFactories {

    private TemporalFactories() {
    }

    public static OloKernelWorkflow defaultWorkflow() {
        return new OloKernelWorkflowImpl();
    }

    public static OloKernelActivities defaultActivities() {
        return new OloKernelActivitiesImpl();
    }

    public static OloKernelDynamicActivity defaultDynamicActivity() {
        return new OloKernelDynamicActivity();
    }
}
