/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.bootstrap;

/**
 * Bootstrap environment variables and system properties used only to locate the configuration storage medium.
 *
 * <p>Runtime worker settings (port, scan folder, cache host, etc.) live inside the loaded
 * {@link org.olo.worker.config.model.WorkerConfiguration} document — not in these variables.
 */
public final class WorkerConfigurationBootstrap {

    /** Selects the configuration backend: {@code FILE}, {@code DATABASE}, {@code REDIS}, {@code GITHUB}. */
    public static final String ENV_SOURCE = "OLO_WORKER_CONFIG_SOURCE";
    public static final String PROP_SOURCE = "olo.worker.config.source";

    /** Path to the worker configuration file when {@code OLO_WORKER_CONFIG_SOURCE=FILE}. */
    public static final String ENV_FILE_PATH = "OLO_WORKER_CONFIG_PATH";
    public static final String PROP_FILE_PATH = "olo.worker.config.path";

    public static final String DEFAULT_SOURCE = "FILE";
    public static final String DEFAULT_FILE_PATH = "worker-config.yaml";

    private WorkerConfigurationBootstrap() {
    }
}
