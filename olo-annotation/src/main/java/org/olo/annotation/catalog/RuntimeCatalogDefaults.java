package org.olo.annotation.catalog;

import org.olo.spi.runtime.RuntimeCapabilities;

import java.util.List;

/**
 * Studio catalog inheritance helpers — delegates to {@link RuntimeCapabilities} (canonical contract).
 *
 * @deprecated Use {@link RuntimeCapabilities} directly; retained for catalog package discoverability.
 */
@Deprecated
public final class RuntimeCatalogDefaults {

    public static final List<String> CAPABILITIES = RuntimeCapabilities.inheritedCatalogDefaultNames();

    private RuntimeCatalogDefaults() {
    }

    public static List<String> materializeDeviations(List<String> effective) {
        return RuntimeCapabilities.materializeDeviationsFromNames(effective);
    }

    public static List<String> resolveEffective(List<String> declared) {
        return RuntimeCapabilities.resolveEffectiveNames(declared);
    }
}
