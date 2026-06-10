package org.olo.worker;

import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.source.ConfigurationSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * OLO Temporal worker entry point.
 */
public final class WorkerApplication {

    private static final Logger log = LoggerFactory.getLogger(WorkerApplication.class);

    private WorkerApplication() {
    }

    public static void main(String[] args) {
        try {
            configureWorkerConfig(args);
            WorkerRuntimeContext context = WorkerBootstrap.start();
            log.info(
                    "OLO worker running: id={}, workflows={}, temporal={}",
                    context.settings().id(),
                    context.workflowRegistry().getWorkflows().size(),
                    context.settings().temporal() != null
                            ? context.settings().temporal().getTarget()
                            : "localhost:7233");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown signal received");
                WorkerBootstrap.shutdown();
            }, "olo-worker-shutdown"));
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OLO worker interrupted", e);
        } catch (RuntimeException e) {
            log.error("OLO worker failed to start", e);
            System.exit(1);
        }
    }

    private static void configureWorkerConfig(String[] args) {
        if (args.length > 0) {
            Path configPath = Path.of(args[0]);
            log.info("Using worker configuration file from argument: {}", configPath.toAbsolutePath().normalize());
            WorkerConfigurationProvider.configure(ConfigurationSourceFactory.forFile(configPath));
            return;
        }

        Path resolved = ConfigurationSourceFactory.resolveFilePath();
        log.info(
                "Using worker configuration from bootstrap defaults: {}",
                resolved.toAbsolutePath().normalize());
    }
}
