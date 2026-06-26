package org.olo.kernel.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.olo.annotation.OloQueueBinding;
import org.olo.annotation.OloWorkflowType;
import org.olo.input.model.WorkflowInput;

/**
 * Temporal workflow entry point for OLO queue execution ({@code workflowType=olo}).
 *
 * <p>The start argument is a {@link WorkflowInput} JSON object (not a JSON-encoded string).
 */
@OloWorkflowType(
        id = "olo",
        label = "OLO Kernel",
        description = "Standard olo-kernel Temporal workflow",
        temporalMethod = "olo",
        queues = {
                @OloQueueBinding(name = "oloQueue1", label = "OLO Queue 1", description = "Architect and planning workflows"),
                @OloQueueBinding(name = "oloQueue2", label = "OLO Queue 2", description = "Agent and execution workflows")
        })
@WorkflowInterface
public interface OloKernelWorkflow {

    @WorkflowMethod(name = "olo")
    String execute(WorkflowInput input);
}
