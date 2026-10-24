/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal.impl;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.human.HumanResumeInput;
import org.olo.kernel.temporal.KernelActivityOperations;
import org.olo.kernel.temporal.OloKernelWorkflow;
import org.olo.kernel.temporal.WorkflowInlineTraversal;
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
    private KernelExecutionSnapshot lastRecordedSnapshot;

    private static ActivityStub activityStub(Duration timeout) {
        return Workflow.newUntypedActivityStub(
                org.olo.kernel.temporal.KernelActivityOptions.of(timeout));
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
        try {
        KernelExecutionSnapshot snapshot = org.olo.kernel.knowledge.GovernedActivity.execute(CONTEXT_BUILD_TIMEOUT,
                        NodeActivityNaming.formatQueue(queue),
                        KernelExecutionSnapshot.class,
                        queue,
                        input);

            while (true) {
                snapshot = traverseUntilTerminal(snapshot);

                if (!snapshot.isWaiting()) {
                    break;
                }

                org.olo.kernel.knowledge.GovernedActivity.execute(HUMAN_WAIT_TIMEOUT,
                                snapshot.getWorkflowActivityName(),
                                Void.class,
                                snapshot,
                                KernelActivityOperations.REPORT_HUMAN_WAITING);

                Workflow.await(() -> pendingHumanInput != null);
                HumanResumeInput resumeInput = pendingHumanInput;
                pendingHumanInput = null;

                snapshot = org.olo.kernel.knowledge.GovernedActivity.execute(HUMAN_WAIT_TIMEOUT,
                                snapshot.getWorkflowActivityName(),
                                KernelExecutionSnapshot.class,
                                snapshot,
                                KernelActivityOperations.RESUME_HUMAN_INPUT,
                                resumeInput);
            }

            return org.olo.kernel.knowledge.GovernedActivity.checked(RESULT_TIMEOUT,snapshot.getWorkflowActivityName(), String.class, snapshot);
        } catch (io.temporal.failure.CanceledFailure cancelled) {
            Workflow.newDetachedCancellationScope(() -> {
                try { activityStub(Duration.ofMinutes(1)).execute(NodeActivityNaming.formatQueue(queue),Void.class,input,"knowledge-outcome:cancellation"); }
                catch (RuntimeException markerFailure) { cancelled.addSuppressed(markerFailure); }
                try { if (lastRecordedSnapshot != null) activityStub(Duration.ofMinutes(1)).execute(
                        lastRecordedSnapshot.getWorkflowActivityName(), Void.class, lastRecordedSnapshot,
                        KernelActivityOperations.CANCEL_TOOLS); }
                catch (RuntimeException cleanupFailure) { cancelled.addSuppressed(cleanupFailure); }
            }).run();
            throw cancelled;
        } catch(RuntimeException failed) {
            Workflow.newDetachedCancellationScope(()->{
                try { activityStub(Duration.ofMinutes(1)).execute(NodeActivityNaming.formatQueue(queue),Void.class,input,"knowledge-outcome:failure"); }
                catch(RuntimeException markerFailure) {failed.addSuppressed(markerFailure);}
            }).run();
            throw failed;
        }
    }

    private KernelExecutionSnapshot traverseUntilTerminal(KernelExecutionSnapshot snapshot) {
        while (!snapshot.isTerminal()) {
            lastRecordedSnapshot = snapshot;
            if (snapshot.isNextRequiresDedicatedActivity()) {
                snapshot = org.olo.kernel.knowledge.GovernedActivity.checked(NODE_STEP_TIMEOUT,
                                snapshot.getNextActivityName(),
                                KernelExecutionSnapshot.class,
                                snapshot,
                                KernelActivityOperations.STEP);
                // M5.5: a step that waits (an approval) asks to resume later; the workflow sleeps durably meanwhile.
                Object resumeAfter = snapshot.getVariables().get(org.olo.kernel.agent.tool.AgentToolLoop.RESUME_AFTER_VARIABLE);
                if (resumeAfter instanceof Number delay && delay.longValue() > 0) {
                    Workflow.sleep(java.time.Duration.ofMillis(delay.longValue()));
                }
            } else {
                snapshot = WorkflowInlineTraversal.executeStep(snapshot);
            }
        }
        return snapshot;
    }
}
