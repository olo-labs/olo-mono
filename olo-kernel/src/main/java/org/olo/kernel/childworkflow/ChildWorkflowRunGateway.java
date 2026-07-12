/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.childworkflow;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.temporal.KernelWorkflowLogger;
import org.olo.kernel.temporal.OloKernelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Runs a referenced child workflow and blocks until it completes.
 * Uses a Temporal child workflow stub when called from a workflow thread; otherwise runs in-process.
 */
public final class ChildWorkflowRunGateway {

    private static final Logger log = LoggerFactory.getLogger(ChildWorkflowRunGateway.class);

    private ChildWorkflowRunGateway() {
    }

    public static String execute(String queue, String childWorkflowId, WorkflowInput childInput) {
        return execute(queue, childWorkflowId, childInput, null);
    }

    public static String execute(
            String queue, String childWorkflowId, WorkflowInput childInput, String parentWorkflowId) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(childWorkflowId, "childWorkflowId");
        Objects.requireNonNull(childInput, "childInput");

        String transactionId = transactionIdFromInput(childInput);
        KernelWorkflowLogger.info(
                ChildWorkflowRunGateway.class,
                "Child workflow dispatch start: queue={}, childWorkflowId={}, parentWorkflowId={}, transactionId={}, executionMode={}",
                queue,
                childWorkflowId,
                parentWorkflowId,
                transactionId,
                inTemporalWorkflow() ? "temporal-child" : "in-process");

        try {
            String result;
            if (inTemporalWorkflow()) {
                result = executeTemporalChild(queue, childWorkflowId, childInput);
            } else {
                result = executeInProcess(queue, childInput);
            }
            KernelWorkflowLogger.info(
                    ChildWorkflowRunGateway.class,
                    "Child workflow dispatch complete: queue={}, childWorkflowId={}, parentWorkflowId={}, transactionId={}, resultLen={}",
                    queue,
                    childWorkflowId,
                    parentWorkflowId,
                    transactionId,
                    result != null ? result.length() : 0);
            return result;
        } catch (RuntimeException e) {
            log.error(
                    "Child workflow dispatch failed: queue={}, childWorkflowId={}, parentWorkflowId={}, transactionId={}",
                    queue,
                    childWorkflowId,
                    parentWorkflowId,
                    transactionId,
                    e);
            throw e;
        }
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

    private static String transactionIdFromInput(WorkflowInput input) {
        if (input.getRouting() == null || input.getRouting().getTransactionId() == null) {
            return null;
        }
        String transactionId = input.getRouting().getTransactionId().trim();
        return transactionId.isBlank() ? null : transactionId;
    }
}
