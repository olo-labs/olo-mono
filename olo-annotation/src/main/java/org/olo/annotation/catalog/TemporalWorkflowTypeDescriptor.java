/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

/**
 * Studio catalog entry for a Temporal workflow type ({@code @OloWorkflowType}).
 */
public final class TemporalWorkflowTypeDescriptor {

    public String id;
    public String label;
    public String description;
    public String temporalMethod;
    public String workflowInterface;
}
