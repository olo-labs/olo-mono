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
    FAILED
}
