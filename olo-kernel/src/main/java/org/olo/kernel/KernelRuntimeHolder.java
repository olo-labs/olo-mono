package org.olo.kernel;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;

import java.util.Objects;

/**
 * Holds the workflow definition registry for kernel activities running inside the Temporal worker.
 */
public final class KernelRuntimeHolder {

    private static volatile WorkflowDefinitionRegistry registry;

    private KernelRuntimeHolder() {
    }

    public static void setRegistry(WorkflowDefinitionRegistry workflowRegistry) {
        registry = Objects.requireNonNull(workflowRegistry, "workflowRegistry");
    }

    public static WorkflowDefinitionRegistry registry() {
        WorkflowDefinitionRegistry current = registry;
        if (current == null) {
            throw new IllegalStateException("workflow definition registry not initialized");
        }
        return current;
    }

    public static void reset() {
        registry = null;
    }
}
