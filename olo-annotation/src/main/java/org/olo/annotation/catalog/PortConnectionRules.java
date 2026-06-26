package org.olo.annotation.catalog;

import org.olo.spi.port.PortSchemaCompatibility;

import java.util.LinkedHashMap;
import java.util.List;
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
        Map<String, Object> rules = new LinkedHashMap<>(PortSchemaCompatibility.catalogConnectionRules());
        rules.put("wireTypes", catalogWireTypes());
        return rules;
    }

    /** Mirrors {@code org.olo.definition.port.PortWireType} without a compile dependency on olo-definition. */
    public static List<String> catalogWireTypes() {
        return List.of("any", "message", "capabilities", "agent-plug");
    }
}
