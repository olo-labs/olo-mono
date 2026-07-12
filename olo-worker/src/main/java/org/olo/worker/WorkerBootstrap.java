/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker;

import io.temporal.worker.WorkerFactory;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;
import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.WorkerSettings;
import org.olo.worker.llm.LlmServerUnavailableException;
import org.olo.worker.llm.WorkerLlmHealthCheck;
import org.olo.worker.temporal.TemporalConnectionErrors;
import org.olo.worker.temporal.TemporalServerUnavailableException;
import org.olo.worker.temporal.TemporalWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Boots the OLO worker: loads configuration, builds the workflow definition cache, and starts Temporal workers.
 */
public final class WorkerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(WorkerBootstrap.class);

    private static volatile WorkerRuntimeContext context;
    private static volatile WorkerFactory workerFactory;

    private WorkerBootstrap() {
    }

    /**
     * First call loads worker configuration and workflow definitions into memory. Subsequent calls return
     * the cached context unless {@code refresh} is {@code true}.
     */
    public static WorkerRuntimeContext start() {
        return start(false);
    }

    /**
     * Boots or refreshes the worker runtime.
     */
    public static WorkerRuntimeContext start(boolean refresh) {
        if (!refresh && context != null) {
            log.debug("Returning cached worker runtime context");
            return context;
        }

        synchronized (WorkerBootstrap.class) {
            if (!refresh && context != null) {
                log.debug("Returning cached worker runtime context");
                return context;
            }

            log.info("Starting OLO worker bootstrap (refresh={})", refresh);

            WorkerSettings settings = loadConfiguration(refresh);
            Path scanFolder = resolveScanFolder(settings);
            WorkflowDefinitionRegistry registry = loadWorkflowRegistry(scanFolder, settings, refresh);
            verifyLlmEndpoints(registry);
            startTemporalWorkers(settings, registry);

            context = new WorkerRuntimeContext(settings, registry);
            log.info(
                    "OLO worker bootstrap complete: workerId={}, workflows={}, queues={}",
                    settings.id(),
                    registry.getWorkflows().size(),
                    registry.getWorkflowsByQueue().size());
            return context;
        }
    }

    /**
     * Returns the cached runtime context after {@link #start()}.
     */
    public static WorkerRuntimeContext context() {
        WorkerRuntimeContext current = context;
        if (current == null) {
            log.error("Worker runtime context requested before bootstrap completed");
            throw new IllegalStateException("worker not started; call WorkerBootstrap.start() first");
        }
        return current;
    }

    /**
     * Stops Temporal workers and clears cached runtime state.
     */
    public static void shutdown() {
        synchronized (WorkerBootstrap.class) {
            log.info("Shutting down OLO worker");
            WorkerRefreshMonitor.stop();
            stopTemporalWorkers();
            context = null;
            WorkerConfigurationProvider.reset();
            OloBootstrap.reset();
            log.info("OLO worker shutdown complete");
        }
    }

    private static WorkerSettings loadConfiguration(boolean refresh) {
        log.info("Step 1/5: Loading worker configuration (refresh={})", refresh);
        try {
            WorkerSettings settings = WorkerConfigurationProvider.load(refresh);
            log.info(
                    "Step 1/5 complete: workerId={}, serverPort={}, temporalTarget={}",
                    settings.id(),
                    settings.serverPort(),
                    settings.temporal() != null ? settings.temporal().getTarget() : "localhost:7233");
            return settings;
        } catch (RuntimeException e) {
            log.error(WorkerBootstrapStep.CONFIGURATION.failureMessage(), e);
            throw e;
        }
    }

    private static Path resolveScanFolder(WorkerSettings settings) {
        log.info("Step 2/5: Resolving workflowDefinitions.scanFolder");
        try {
            Path baseDirectory = WorkerConfigurationProvider.configurationBaseDirectory();
            Path scanFolder = settings.resolvedWorkflowDefinitionsScanFolder(baseDirectory);
            log.info(
                    "Step 2/5 complete: scanFolder={}, baseDirectory={}, recursive={}",
                    scanFolder,
                    baseDirectory,
                    settings.workflowDefinitionsRecursive());
            if (!Files.isDirectory(scanFolder)) {
                throw new IllegalStateException("workflow definition scan folder does not exist: " + scanFolder);
            }
            return scanFolder;
        } catch (RuntimeException e) {
            log.error(WorkerBootstrapStep.WORKFLOW_SCAN_FOLDER.failureMessage(), e);
            throw e;
        }
    }

    private static WorkflowDefinitionRegistry loadWorkflowRegistry(
            Path scanFolder,
            WorkerSettings settings,
            boolean refresh) {
        log.info("Step 3/5: Loading workflow definitions from {}", scanFolder);
        try {
            WorkflowDefinitionRegistry registry = OloBootstrap.load(
                    scanFolder,
                    settings.workflowDefinitionsRecursive(),
                    refresh);
            log.info(
                    "Step 3/5 complete: loaded {} workflow(s) across {} queue(s)",
                    registry.getWorkflows().size(),
                    registry.getWorkflowsByQueue().size());
            logLoadedNodeActivityNames(registry);
            return registry;
        } catch (RuntimeException e) {
            log.error(WorkerBootstrapStep.WORKFLOW_REGISTRY.failureMessage() + " scanFolder=" + scanFolder, e);
            throw e;
        }
    }

    private static void verifyLlmEndpoints(WorkflowDefinitionRegistry registry) {
        log.info("Step 4/5: Verifying LLM endpoints");
        try {
            WorkerLlmHealthCheck.verify(registry);
            log.info("Step 4/5 complete: LLM endpoints reachable");
        } catch (LlmServerUnavailableException e) {
            log.error(e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error(WorkerBootstrapStep.LLM_ENDPOINT.failureMessage(), e);
            throw e;
        }
    }

    private static void startTemporalWorkers(WorkerSettings settings, WorkflowDefinitionRegistry registry) {
        if (Boolean.getBoolean("olo.worker.skipTemporal")) {
            log.warn("Step 5/5 skipped: Temporal startup disabled (olo.worker.skipTemporal=true)");
            return;
        }

        String target = settings.temporal() != null && settings.temporal().getTarget() != null
                ? settings.temporal().getTarget()
                : "localhost:7233";
        String namespace = settings.temporal() != null && settings.temporal().getNamespace() != null
                ? settings.temporal().getNamespace()
                : "default";

        log.info(
                "Step 5/5: Connecting to Temporal at target={}, namespace={}, queues={}",
                target,
                namespace,
                registry.getWorkflowsByQueue().keySet());
        try {
            stopTemporalWorkers();
            workerFactory = TemporalWorkerFactory.start(settings, registry);
            log.info("Step 5/5 complete: Temporal workers started for {} queue(s)", registry.getWorkflowsByQueue().size());
        } catch (RuntimeException e) {
            if (TemporalConnectionErrors.isServerUnavailable(e)) {
                TemporalServerUnavailableException unavailable =
                        new TemporalServerUnavailableException(target, namespace, e);
                log.error(unavailable.getMessage());
                throw unavailable;
            }
            log.error(
                    WorkerBootstrapStep.TEMPORAL.failureMessage() + " target=" + target + ", namespace=" + namespace,
                    e);
            throw e;
        }
    }

    private static void stopTemporalWorkers() {
        if (workerFactory != null) {
            log.info("Stopping existing Temporal worker factory");
            workerFactory.shutdown();
            workerFactory = null;
        }
    }

    private static void logLoadedNodeActivityNames(WorkflowDefinitionRegistry registry) {
        for (WorkflowDefinition workflow : registry.getWorkflowsByQueue().values()) {
            if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
                continue;
            }
            StringBuilder names = new StringBuilder();
            for (NodeDefinition node : workflow.getNodes()) {
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(NodeActivityNaming.formatNode(node));
            }
            log.info(
                    "Loaded workflow queue={} id={} nodeActivities=[{}]",
                    workflow.getQueue(),
                    workflow.getId(),
                    names);
        }
    }
}
