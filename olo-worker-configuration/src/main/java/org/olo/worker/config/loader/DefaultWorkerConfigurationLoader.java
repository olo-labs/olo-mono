/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.loader;

import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.source.ConfigurationSource;
import org.olo.worker.config.validation.WorkerConfigurationValidator;

import java.util.Objects;

/**
 * Default loader that reads worker configuration from a {@link ConfigurationSource}.
 */
public final class DefaultWorkerConfigurationLoader implements WorkerConfigurationLoader {

    private final ConfigurationSource source;
    private final boolean validate;

    public DefaultWorkerConfigurationLoader(ConfigurationSource source) {
        this(source, true);
    }

    public DefaultWorkerConfigurationLoader(ConfigurationSource source, boolean validate) {
        this.source = Objects.requireNonNull(source, "source");
        this.validate = validate;
    }

    @Override
    public WorkerConfiguration load() {
        WorkerConfiguration configuration = source.load();
        if (validate) {
            WorkerConfigurationValidator.validateOrThrow(configuration);
        }
        return configuration;
    }

    public static WorkerConfigurationLoader fromSource(ConfigurationSource source) {
        return new DefaultWorkerConfigurationLoader(source);
    }
}
