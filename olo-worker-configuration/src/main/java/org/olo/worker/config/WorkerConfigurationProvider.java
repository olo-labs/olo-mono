/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config;

import org.olo.worker.config.loader.DefaultWorkerConfigurationLoader;
import org.olo.worker.config.loader.WorkerConfigurationLoader;
import org.olo.worker.config.model.ConfigurationSourceType;
import org.olo.worker.config.model.WorkerConfiguration;
import org.olo.worker.config.source.ConfigurationSource;
import org.olo.worker.config.source.ConfigurationSourceFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Single entry point for worker processes to obtain configuration.
 *
 * <p>Call {@link #load()} once at startup to load and cache settings. Subsequent calls return the
 * in-memory cache without re-reading storage. Call {@link #load(boolean)} with {@code refresh=true}
 * to reload from the active source at runtime.
 */
public final class WorkerConfigurationProvider {

    private static final Object LOCK = new Object();

    private static volatile ConfigurationSource configuredSource;
    private static volatile WorkerConfiguration cachedConfiguration;
    private static volatile WorkerSettings cachedSettings;

    private WorkerConfigurationProvider() {
    }

    /**
     * Returns cached worker settings, loading from the active source on the first call.
     */
    public static WorkerSettings load() {
        return load(false);
    }

    /**
     * Returns worker settings. When {@code refresh} is {@code false}, returns the in-memory cache
     * after the first load. When {@code refresh} is {@code true}, reloads from storage and replaces
     * the cache.
     */
    public static WorkerSettings load(boolean refresh) {
        synchronized (LOCK) {
            if (refresh || cachedSettings == null) {
                cachedConfiguration = createLoader().load();
                cachedSettings = new WorkerSettings(cachedConfiguration);
            }
            return cachedSettings;
        }
    }

    /**
     * Directory containing the worker configuration file when the active source is {@code FILE}.
     * Used to resolve relative paths such as {@code workflowDefinitions.scanFolder}.
     */
    public static Path configurationBaseDirectory() {
        ConfigurationSource source = activeSource();
        if (source.getSourceType() == ConfigurationSourceType.FILE) {
            return Path.of(source.getSourceId()).getParent();
        }
        return Path.of(System.getProperty("user.dir"));
    }

    /**
     * Forces the provider to use a specific source (tests and explicit worker bootstrap).
     */
    public static void configure(ConfigurationSource source) {
        Objects.requireNonNull(source, "source");
        synchronized (LOCK) {
            configuredSource = source;
            cachedConfiguration = null;
            cachedSettings = null;
        }
    }

    /**
     * Clears cached configuration and any configured source.
     */
    public static void reset() {
        synchronized (LOCK) {
            configuredSource = null;
            cachedConfiguration = null;
            cachedSettings = null;
        }
    }

    private static WorkerConfigurationLoader createLoader() {
        return DefaultWorkerConfigurationLoader.fromSource(activeSource());
    }

    private static ConfigurationSource activeSource() {
        return configuredSource != null ? configuredSource : ConfigurationSourceFactory.fromBootstrap();
    }
}
