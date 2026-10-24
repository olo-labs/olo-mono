/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.node;

/**
 * High-level execution status returned by a {@link Node}.
 */
public enum NodeStatus {
    COMPLETED,
    WAITING,
    /**
     * The node recorded durable progress and runs again at the next step. Under Temporal that step is a new activity,
     * so what this step recorded is in workflow history before anything it decided is acted on (M5.1).
     */
    CONTINUE,
    FAILED
}
