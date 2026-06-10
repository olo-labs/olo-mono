package org.olo.kernel.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.olo.input.model.WorkflowInput;

/**
 * Temporal workflow entry point for OLO queue execution ({@code workflowType=olo}).
 *
 * <p>The start argument is a {@link WorkflowInput} JSON object (not a JSON-encoded string).
 */
@WorkflowInterface
public interface OloKernelWorkflow {

    @WorkflowMethod(name = "olo")
    String execute(WorkflowInput input);
}
