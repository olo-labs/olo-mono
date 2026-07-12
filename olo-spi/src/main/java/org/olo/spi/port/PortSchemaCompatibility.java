/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.port;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Definition-time port wiring rules for workflow editors and validators.
 * <p>
 * Connect output {@code schema} on the source port to input {@code schema} on the target port.
 * Examples: {@code string → string} ✓, {@code string → number} ✗, {@code Stock[] → Stock[]} ✓.
 */
public final class PortSchemaCompatibility {

    public static final String WILDCARD_ANY = "any";

    public static final List<String> WILDCARDS = List.of("any", "*");

    public static final List<String> PRIMITIVES =
            List.of("string", "number", "integer", "boolean", "object", "array");

    private static final Map<String, String> PRIMITIVE_ALIASES = Map.ofEntries(
            Map.entry("string", "string"),
            Map.entry("str", "string"),
            Map.entry("number", "number"),
            Map.entry("double", "number"),
            Map.entry("float", "number"),
            Map.entry("decimal", "number"),
            Map.entry("integer", "integer"),
            Map.entry("int", "integer"),
            Map.entry("long", "integer"),
            Map.entry("boolean", "boolean"),
            Map.entry("bool", "boolean"),
            Map.entry("object", "object"),
            Map.entry("json", "object"),
            Map.entry("map", "object"),
            Map.entry("array", "array"),
            Map.entry("list", "array"));

    private PortSchemaCompatibility() {}

    /**
     * Returns whether an output port may wire to an input port at definition time.
     * No implicit coercion — types must match structurally after canonicalization.
     */
    public static boolean compatible(String outputSchema, String inputSchema) {
        if (isBlank(outputSchema) || isBlank(inputSchema)) {
            return false;
        }
        String output = canonicalize(outputSchema);
        String input = canonicalize(inputSchema);
        if (acceptsAny(input) || acceptsAny(output)) {
            return true;
        }
        return output.equals(input);
    }

    /**
     * Canonical form used for comparisons. Primitive aliases normalize case; domain types keep author casing.
     */
    public static String canonicalize(String schema) {
        if (schema == null) {
            return "";
        }
        String trimmed = schema.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (acceptsAny(trimmed)) {
            return WILDCARD_ANY;
        }
        if (trimmed.endsWith("[]")) {
            String element = trimmed.substring(0, trimmed.length() - 2).trim();
            return canonicalizeElement(element) + "[]";
        }
        return canonicalizeElement(trimmed);
    }

    /** Catalog defaults describing connection rules for Studio clients. */
    public static Map<String, Object> catalogConnectionRules() {
        return Map.of(
                "version", "1.0",
                "strategy", "schema_match",
                "wildcards", WILDCARDS,
                "primitives", PRIMITIVES);
    }

    private static String canonicalizeElement(String element) {
        String primitive = primitiveAlias(element);
        if (primitive != null) {
            return primitive;
        }
        return element;
    }

    private static String primitiveAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return PRIMITIVE_ALIASES.get(raw.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean acceptsAny(String schema) {
        if (schema == null || schema.isBlank()) {
            return false;
        }
        String trimmed = schema.trim();
        return WILDCARD_ANY.equalsIgnoreCase(trimmed) || "*".equals(trimmed);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
