package org.olo.kernel.temporal;

import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;

public final class OloKernelActivitiesImpl implements OloKernelActivities {

    @Override
    public String buildContextAndNotifyUi(String queue, WorkflowInput input) {
        return KernelEntryPoint.execute(queue, input, KernelRuntimeHolder.registry());
    }
}
