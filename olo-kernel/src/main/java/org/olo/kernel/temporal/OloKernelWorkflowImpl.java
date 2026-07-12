/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.human.HumanResumeInput;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;

import java.time.Duration;
import java.util.Map;

/**
 * Temporal workflow that orchestrates queue execution: one activity per node by default,
 * with explicitly INLINE nodes executed synchronously inside this workflow loop.
 */
public final class OloKernelWorkflowImpl implements OloKernelWorkflow {

    private static final Duration CONTEXT_BUILD_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration NODE_STEP_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration HUMAN_WAIT_TIMEOUT = Duration.ofHours(24);
    private static final Duration RESULT_TIMEOUT = Duration.ofMinutes(1);

    private HumanResumeInput pendingHumanInput;

    private static ActivityStub activityStub(Duration timeout) {
        return Workflow.newUntypedActivityStub(
                ActivityOptions.newBuilder().setStartToCloseTimeout(timeout).build());
    }

    @Override
    public void submitHumanInput(HumanResumeInput input) {
        pendingHumanInput = input;
    }

    @Override
    public void humanInput(boolean approved, String message) {
        if (!approved) {
            pendingHumanInput = new HumanResumeInput(
                    message != null ? message : "Cancelled by operator",
                    "operator",
                    Map.of("approved", false));
            return;
        }
        pendingHumanInput = HumanResumeInput.fromOperatorMessage(
                message != null ? message : "",
                "operator");
    }

    @Override
    public String execute(WorkflowInput input) {
        String queue = Workflow.getInfo().getTaskQueue();
        KernelExecutionSnapshot snapshot = activityStub(CONTEXT_BUILD_TIMEOUT)
                .execute(
                        NodeActivityNaming.formatQueue(queue),
                        KernelExecutionSnapshot.class,
                        queue,
                        input);

        while (true) {
            snapshot = traverseUntilTerminal(snapshot);

            if (!snapshot.isWaiting()) {
                break;
            }

            activityStub(HUMAN_WAIT_TIMEOUT)
                    .execute(
                            snapshot.getWorkflowActivityName(),
                            Void.class,
                            snapshot,
                            KernelActivityOperations.REPORT_HUMAN_WAITING);

            Workflow.await(() -> pendingHumanInput != null);
            HumanResumeInput resumeInput = pendingHumanInput;
            pendingHumanInput = null;

            snapshot = activityStub(HUMAN_WAIT_TIMEOUT)
                    .execute(
                            snapshot.getWorkflowActivityName(),
                            KernelExecutionSnapshot.class,
                            snapshot,
                            KernelActivityOperations.RESUME_HUMAN_INPUT,
                            resumeInput);
        }

        return activityStub(RESULT_TIMEOUT)
                .execute(snapshot.getWorkflowActivityName(), String.class, snapshot);
    }

    private static KernelExecutionSnapshot traverseUntilTerminal(KernelExecutionSnapshot snapshot) {
        while (!snapshot.isTerminal()) {
            if (snapshot.isNextRequiresDedicatedActivity()) {
                snapshot = activityStub(NODE_STEP_TIMEOUT)
                        .execute(
                                snapshot.getNextActivityName(),
                                KernelExecutionSnapshot.class,
                                snapshot,
                                KernelActivityOperations.STEP);
            } else {
                snapshot = WorkflowInlineTraversal.executeStep(snapshot);
            }
        }
        return snapshot;
    }
}
