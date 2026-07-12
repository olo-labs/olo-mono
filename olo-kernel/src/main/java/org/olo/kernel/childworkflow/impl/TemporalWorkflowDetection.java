/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.childworkflow.impl;

import io.temporal.workflow.Workflow;

final class TemporalWorkflowDetection {

    private TemporalWorkflowDetection() {
    }

    static boolean inTemporalWorkflow() {
        try {
            Workflow.getInfo();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
