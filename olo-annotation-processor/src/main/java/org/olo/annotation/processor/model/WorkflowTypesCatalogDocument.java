/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.model;

import org.olo.annotation.catalog.TemporalQueueDescriptor;
import org.olo.annotation.catalog.TemporalWorkflowTypeDescriptor;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowTypesCatalogDocument {

    public String schemaVersion = "1.0";
    public String moduleId;
    public String catalogType = "workflow-types";
    public String generatedAt;
    public List<TemporalWorkflowTypeDescriptor> workflowTypes = new ArrayList<>();
    public List<TemporalQueueDescriptor> queues = new ArrayList<>();
}
