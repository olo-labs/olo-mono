package org.olo.worker.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.temporal.KernelWorkflowRegistrar;
import org.olo.worker.config.WorkerSettings;
import org.olo.worker.config.model.TemporalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Creates Temporal workers for each task queue and registers {@code olo-kernel} as the execution entry point.
 */
public final class TemporalWorkerFactory {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorkerFactory.class);

    private TemporalWorkerFactory() {
    }

    public static WorkerFactory start(WorkerSettings settings, WorkflowDefinitionRegistry registry) {
        TemporalSettings temporal = settings.temporal();
        String target = temporal != null && temporal.getTarget() != null && !temporal.getTarget().isBlank()
                ? temporal.getTarget()
                : "localhost:7233";
        String namespace = temporal != null && temporal.getNamespace() != null && !temporal.getNamespace().isBlank()
                ? temporal.getNamespace()
                : "default";

        log.info("Creating Temporal service client for target={}", target);
        WorkflowServiceStubs service;
        try {
            service = WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
        } catch (RuntimeException e) {
            log.error("Failed to create Temporal service client for target={}", target, e);
            throw e;
        }

        WorkflowClient client = WorkflowClient.newInstance(
                service,
                WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Set<String> queues = new LinkedHashSet<>();
        for (WorkflowDefinition definition : registry.getWorkflowsByQueue().values()) {
            if (definition.getQueue() != null && !definition.getQueue().isBlank()) {
                queues.add(definition.getQueue());
            }
        }

        if (queues.isEmpty()) {
            log.warn("No Temporal task queues found in workflow registry; no workers will poll");
        }

        for (String queue : queues) {
            String workflowType = registry.resolveWorkflowTypeForQueue(queue);
            log.info("Registering olo-kernel on Temporal queue '{}' (workflowType={})", queue, workflowType);
            try {
                Worker worker = factory.newWorker(queue);
                KernelWorkflowRegistrar.register(worker, registry);
            } catch (RuntimeException e) {
                log.error("Failed to register olo-kernel on queue '{}' (workflowType={})", queue, workflowType, e);
                throw e;
            }
        }

        log.info("Starting Temporal worker factory");
        try {
            factory.start();
        } catch (RuntimeException e) {
            log.error(
                    "Failed to start Temporal worker factory for target={}, namespace={}, queues={}",
                    target,
                    namespace,
                    queues,
                    e);
            throw e;
        }
        return factory;
    }
}
