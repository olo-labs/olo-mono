/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes kernel diagnostics to SLF4J on worker threads and to Temporal workflow logs in the workflow thread.
 */
public final class KernelWorkflowLogger {

    private KernelWorkflowLogger() {
    }

    public static void info(Class<?> type, String message, Object... args) {
        if (inTemporalWorkflow()) {
            Workflow.getLogger(type).info(message, args);
        } else {
            LoggerFactory.getLogger(type).info(message, args);
        }
    }

    private static boolean inTemporalWorkflow() {
        try {
            Workflow.getInfo();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
