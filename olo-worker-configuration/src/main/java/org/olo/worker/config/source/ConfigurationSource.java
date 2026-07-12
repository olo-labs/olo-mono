/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.source;

import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.ConfigurationSourceType;
import org.olo.worker.config.model.WorkerConfiguration;

/**
 * Pluggable source of worker deployment configuration.
 *
 * <p>Implementations may read from the filesystem today and from database, Redis, or GitHub later.
 */
public interface ConfigurationSource {

    ConfigurationSourceType getSourceType();

    /**
     * Stable identifier for this source instance (file path, connection name, repo ref, etc.).
     */
    String getSourceId();

    WorkerConfiguration load() throws WorkerConfigurationException;
}
