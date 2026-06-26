package org.olo.annotation.catalog;

import java.util.List;

/**
 * Merged Temporal workflow type and queue catalog for Studio.
 */
public final class TemporalWorkflowCatalog {

    public String schemaVersion = "1.0";
    public List<TemporalWorkflowTypeDescriptor> workflowTypes = List.of();
    public List<TemporalQueueDescriptor> queues = List.of();
}
