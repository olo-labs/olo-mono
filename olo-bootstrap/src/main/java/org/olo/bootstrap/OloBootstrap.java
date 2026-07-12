/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.bootstrap;

import org.olo.bootstrap.loader.DirectoryWorkflowDefinitionLoader;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Builds and caches in-memory workflow definition indexes from {@code olo-configuration} folders.
 *
 * <p>Call {@link #load(Path, boolean)} once after worker configuration is loaded. Subsequent calls
 * return the in-memory cache. Call {@link #load(Path, boolean, boolean)} with {@code refresh=true}
 * to rebuild the cache at runtime.
 */
public final class OloBootstrap {

    private static final Object LOCK = new Object();

    private static volatile Path cachedScanFolder;
    private static volatile boolean cachedRecursive;
    private static volatile WorkflowDefinitionRegistry cachedRegistry;
    private static final DirectoryWorkflowDefinitionLoader LOADER = new DirectoryWorkflowDefinitionLoader();

    private OloBootstrap() {
    }

    /**
     * Returns the cached workflow registry, loading on the first call for the given scan folder.
     */
    public static WorkflowDefinitionRegistry load(Path scanFolder, boolean recursive) {
        return load(scanFolder, recursive, false);
    }

    /**
     * Returns the workflow registry. When {@code refresh} is {@code false}, returns the in-memory
     * cache when the scan folder and recursion flag match the previous load. When {@code refresh}
     * is {@code true}, rescans storage and replaces the cache.
     */
    public static WorkflowDefinitionRegistry load(Path scanFolder, boolean recursive, boolean refresh) {
        Objects.requireNonNull(scanFolder, "scanFolder");
        synchronized (LOCK) {
            if (refresh
                    || cachedRegistry == null
                    || !scanFolder.toAbsolutePath().normalize().equals(cachedScanFolder)
                    || cachedRecursive != recursive) {
                cachedScanFolder = scanFolder.toAbsolutePath().normalize();
                cachedRecursive = recursive;
                cachedRegistry = LOADER.load(cachedScanFolder, recursive);
            }
            return cachedRegistry;
        }
    }

    /**
     * Clears the in-memory workflow registry cache.
     */
    public static void reset() {
        synchronized (LOCK) {
            cachedScanFolder = null;
            cachedRecursive = false;
            cachedRegistry = null;
        }
    }
}
