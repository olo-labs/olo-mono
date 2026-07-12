/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates port connections using {@link PortWireType} when {@code type}/{@code acceptType} are set.
 */
public final class PortWireCompatibility {

    private PortWireCompatibility() {
    }

    public static boolean compatible(PortDefinition outputPort, PortDefinition inputPort) {
        if (outputPort == null || inputPort == null) {
            return false;
        }
        String outputWire = wireType(outputPort);
        for (String acceptType : acceptTypes(inputPort)) {
            if (wireTypesCompatible(outputWire, acceptType)) {
                return true;
            }
        }
        return false;
    }

    public static String wireType(PortDefinition port) {
        if (port.getType() != null && !port.getType().isBlank()) {
            return canonicalizeWire(port.getType());
        }
        if (port.getSchema() != null && !port.getSchema().isBlank()) {
            return canonicalizeWire(port.getSchema());
        }
        return PortWireType.ANY.wireName();
    }

    public static List<String> acceptTypes(PortDefinition port) {
        List<String> accepted = new ArrayList<>();
        if (port.getAcceptType() != null && !port.getAcceptType().isBlank()) {
            for (String part : port.getAcceptType().split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    accepted.add(canonicalizeWire(trimmed));
                }
            }
        }
        if (accepted.isEmpty()) {
            accepted.add(wireType(port));
        }
        return List.copyOf(accepted);
    }

    public static boolean wireTypesCompatible(String outputWire, String inputAcceptWire) {
        if (outputWire == null || outputWire.isBlank() || inputAcceptWire == null || inputAcceptWire.isBlank()) {
            return false;
        }
        String output = canonicalizeWire(outputWire);
        String input = canonicalizeWire(inputAcceptWire);
        PortWireType outputType = tryParse(output);
        PortWireType inputType = tryParse(input);
        if (outputType != null && inputType != null) {
            return outputType == inputType;
        }
        if (PortWireType.ANY.wireName().equals(input) || PortWireType.ANY.wireName().equals(output)) {
            return true;
        }
        return output.equals(input);
    }

    public static String canonicalizeWire(String wireType) {
        if (wireType == null) {
            return "";
        }
        String trimmed = wireType.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        PortWireType known = tryParse(trimmed);
        if (known != null) {
            return known.wireName();
        }
        return trimmed;
    }

    public static List<String> catalogWireTypes() {
        return List.of(
                PortWireType.ANY.wireName(),
                PortWireType.MESSAGE.wireName(),
                PortWireType.CAPABILITIES.wireName(),
                PortWireType.AGENT_PLUG.wireName());
    }

    private static PortWireType tryParse(String raw) {
        try {
            return PortWireType.fromWireName(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
