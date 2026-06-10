package org.olo.kernel.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.olo.input.model.WorkflowInput;

import java.time.Duration;

/**
 * Temporal workflow that delegates queue execution to {@link org.olo.kernel.KernelEntryPoint}
 * via {@link OloKernelActivities}.
 */
public final class OloKernelWorkflowImpl implements OloKernelWorkflow {

    private final OloKernelActivities activities = Workflow.newActivityStub(
            OloKernelActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build());

    @Override
    public String execute(WorkflowInput input) {
        String queue = Workflow.getInfo().getTaskQueue();
        return activities.buildContextAndNotifyUi(queue, input);
    }
}
