package org.olo.kernel.childworkflow;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.temporal.OloKernelWorkflow;

import java.util.Objects;

/**
 * Runs a referenced child workflow and blocks until it completes.
 * Uses a Temporal child workflow stub when called from a workflow thread; otherwise runs in-process.
 */
public final class ChildWorkflowRunGateway {

    private ChildWorkflowRunGateway() {
    }

    public static String execute(String queue, String childWorkflowId, WorkflowInput childInput) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(childWorkflowId, "childWorkflowId");
        Objects.requireNonNull(childInput, "childInput");

        if (inTemporalWorkflow()) {
            return executeTemporalChild(queue, childWorkflowId, childInput);
        }
        return executeInProcess(queue, childInput);
    }

    static boolean inTemporalWorkflow() {
        try {
            Workflow.getInfo();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String executeTemporalChild(String queue, String childWorkflowId, WorkflowInput childInput) {
        OloKernelWorkflow child = Workflow.newChildWorkflowStub(
                OloKernelWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setTaskQueue(queue)
                        .setWorkflowId(childWorkflowId + "-" + Workflow.randomUUID())
                        .build());
        return child.execute(childInput);
    }

    private static String executeInProcess(String queue, WorkflowInput childInput) {
        WorkflowDefinitionRegistry registry = KernelRuntimeHolder.registry();
        if (registry.resolve(queue, childWorkflowIdFromInput(childInput)).isEmpty()) {
            throw new KernelException("no workflow definition registered for child workflow: "
                    + childWorkflowIdFromInput(childInput));
        }
        return KernelEntryPoint.execute(queue, childInput, registry);
    }

    private static String childWorkflowIdFromInput(WorkflowInput input) {
        if (input.getRouting() == null || input.getRouting().getPipeline() == null) {
            return null;
        }
        return input.getRouting().getPipeline().trim();
    }
}
