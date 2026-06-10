package org.olo.core.catalog;

import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionCatalogLoader;

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
}
