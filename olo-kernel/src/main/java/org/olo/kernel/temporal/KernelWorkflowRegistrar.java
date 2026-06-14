package org.olo.kernel.temporal;

import io.temporal.worker.Worker;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.kernel.KernelRuntimeHolder;

/**
 * Registers {@link OloKernelWorkflow} as the queue execution entry point on a Temporal worker.
 */
public final class KernelWorkflowRegistrar {

    private KernelWorkflowRegistrar() {
    }

    public static void register(Worker worker, WorkflowDefinitionRegistry registry) {
        KernelRuntimeHolder.setRegistry(registry);
        worker.registerWorkflowImplementationTypes(OloKernelWorkflowImpl.class);
        worker.registerActivitiesImplementations(new OloKernelDynamicActivity());
    }
}
