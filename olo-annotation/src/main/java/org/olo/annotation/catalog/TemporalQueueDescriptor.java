/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

/**
 * Studio catalog entry mapping a Temporal task queue to a workflow type.
 */
public final class TemporalQueueDescriptor {

    public String name;
    public String label;
    public String description;
    public String workflowType;
}
