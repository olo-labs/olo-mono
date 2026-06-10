package org.olo.worker;

/**
 * Named bootstrap steps for consistent log messages when a worker startup phase fails.
 */
enum WorkerBootstrapStep {

    CONFIGURATION(
            "load worker configuration",
            "Verify OLO_WORKER_CONFIG_PATH, pass the config file as the first program argument, "
                    + "or use ../olo-worker-configuration/samples/worker-config.yaml in the monorepo"),
    WORKFLOW_SCAN_FOLDER(
            "resolve workflowDefinitions.scanFolder",
            "Ensure workflowDefinitions.scanFolder in worker config points to an existing directory "
                    + "(relative paths resolve against the config file location)"),
    WORKFLOW_REGISTRY(
            "build workflow definition registry",
            "Check that the scan folder contains valid workflow JSON/YAML files and that ids/queues are unique"),
    TEMPORAL(
            "connect to Temporal and register queue workers",
            "Start Temporal at the configured target (default localhost:7233), or set -Dolo.worker.skipTemporal=true "
                    + "to skip Temporal during local bootstrap-only runs");

    private final String action;
    private final String remediation;

    WorkerBootstrapStep(String action, String remediation) {
        this.action = action;
        this.remediation = remediation;
    }

    String failureMessage() {
        return "Bootstrap failed while attempting to " + action + ". " + remediation;
    }
}
