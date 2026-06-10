package org.olo.worker;

import io.temporal.worker.WorkerFactory;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.WorkerSettings;
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
            stopTemporalWorkers();
            context = null;
            WorkerConfigurationProvider.reset();
            OloBootstrap.reset();
            log.info("OLO worker shutdown complete");
        }
    }

    private static WorkerSettings loadConfiguration(boolean refresh) {
        log.info("Step 1/4: Loading worker configuration (refresh={})", refresh);
        try {
            WorkerSettings settings = WorkerConfigurationProvider.load(refresh);
            log.info(
                    "Step 1/4 complete: workerId={}, serverPort={}, temporalTarget={}",
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
        log.info("Step 2/4: Resolving workflowDefinitions.scanFolder");
        try {
            Path baseDirectory = WorkerConfigurationProvider.configurationBaseDirectory();
            Path scanFolder = settings.resolvedWorkflowDefinitionsScanFolder(baseDirectory);
            log.info(
                    "Step 2/4 complete: scanFolder={}, baseDirectory={}, recursive={}",
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
        log.info("Step 3/4: Loading workflow definitions from {}", scanFolder);
        try {
            WorkflowDefinitionRegistry registry = OloBootstrap.load(
                    scanFolder,
                    settings.workflowDefinitionsRecursive(),
                    refresh);
            log.info(
                    "Step 3/4 complete: loaded {} workflow(s) across {} queue(s)",
                    registry.getWorkflows().size(),
                    registry.getWorkflowsByQueue().size());
            return registry;
        } catch (RuntimeException e) {
            log.error(WorkerBootstrapStep.WORKFLOW_REGISTRY.failureMessage() + " scanFolder=" + scanFolder, e);
            throw e;
        }
    }

    private static void startTemporalWorkers(WorkerSettings settings, WorkflowDefinitionRegistry registry) {
        if (Boolean.getBoolean("olo.worker.skipTemporal")) {
            log.warn("Step 4/4 skipped: Temporal startup disabled (olo.worker.skipTemporal=true)");
            return;
        }

        String target = settings.temporal() != null && settings.temporal().getTarget() != null
                ? settings.temporal().getTarget()
                : "localhost:7233";
        String namespace = settings.temporal() != null && settings.temporal().getNamespace() != null
                ? settings.temporal().getNamespace()
                : "default";

        log.info(
                "Step 4/4: Connecting to Temporal at target={}, namespace={}, queues={}",
                target,
                namespace,
                registry.getWorkflowsByQueue().keySet());
        try {
            stopTemporalWorkers();
            workerFactory = TemporalWorkerFactory.start(settings, registry);
            log.info("Step 4/4 complete: Temporal workers started for {} queue(s)", registry.getWorkflowsByQueue().size());
        } catch (RuntimeException e) {
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
}
