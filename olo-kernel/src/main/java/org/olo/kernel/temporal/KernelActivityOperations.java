/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

/**
 * Operation codes passed to {@link OloKernelDynamicActivity} for per-node traversal steps.
 */
public final class KernelActivityOperations {

    public static final String STEP = "step";
    public static final String REPORT_HUMAN_WAITING = "report-human-waiting";
    public static final String RESUME_HUMAN_INPUT = "resume-human-input";

    private KernelActivityOperations() {
    }
}
