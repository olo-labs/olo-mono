package org.olo.kernel.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.olo.input.model.WorkflowInput;

/**
 * Non-deterministic kernel work executed outside the Temporal workflow sandbox.
 */
@ActivityInterface
public interface OloKernelActivities {

    @ActivityMethod
    String buildContextAndNotifyUi(String queue, WorkflowInput input);
}
