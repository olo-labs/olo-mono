/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.catalog;

import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionCatalogLoader;
import org.olo.annotation.catalog.ExtensionRuntimeRegistry;
import org.olo.annotation.catalog.ExtensionRuntimeRegistryLoader;

/**
 * Loads merged extension metadata generated from {@code @OloNode}, {@code @OloTool}, and {@code @OloHook}
 * for plug-and-play workflow editing UIs.
 */
public final class CoreExtensionCatalog {

    private CoreExtensionCatalog() {
    }

    public static ExtensionCatalog loadMerged() {
        return ExtensionCatalogLoader.loadMerged(CoreExtensionCatalog.class.getClassLoader());
    }

    public static ExtensionRuntimeRegistry loadRuntimeRegistry() {
        return ExtensionRuntimeRegistryLoader.loadMerged(CoreExtensionCatalog.class.getClassLoader());
    }
}
