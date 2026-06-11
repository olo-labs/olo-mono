package org.olo.annotation.catalog;

import java.util.List;

/**
 * Merged runtime bindings from all classpath {@code runtime.json} resources.
 */
public record ExtensionRuntimeRegistry(String schemaVersion, List<RuntimeBindingDescriptor> bindings) {
}
