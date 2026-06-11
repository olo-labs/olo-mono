package org.olo.annotation.catalog;

import org.olo.spi.port.PortSchemaCompatibility;

import java.util.Map;

/**
 * Studio drag-and-drop connection validation — compare output {@code schema} to input {@code schema}.
 */
public final class PortConnectionRules {

    private PortConnectionRules() {}

    public static boolean compatible(String outputSchema, String inputSchema) {
        return PortSchemaCompatibility.compatible(outputSchema, inputSchema);
    }

    public static String canonicalize(String schema) {
        return PortSchemaCompatibility.canonicalize(schema);
    }

    public static Map<String, Object> catalogDefaults() {
        return PortSchemaCompatibility.catalogConnectionRules();
    }
}
