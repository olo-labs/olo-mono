/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.olo.annotation.OloCanvasPorts;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloPortPosition;
import org.olo.annotation.catalog.PortDescriptor;
import org.olo.annotation.catalog.PortUiDescriptor;
import org.olo.annotation.processor.catalog.CatalogPortProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves explicit {@link OloPort} declarations or {@link OloCanvasPorts} profiles into catalog port descriptors.
 */
public final class CatalogPortPopulator {

    public static final String MESSAGE_WIRE = CatalogPortProfiles.MESSAGE_WIRE;
    public static final String CAPABILITIES_WIRE = CatalogPortProfiles.CAPABILITIES_WIRE;
    public static final String AGENT_PLUG_WIRE = CatalogPortProfiles.AGENT_PLUG_WIRE;

    public static final String CAPABILITIES_PORT_ID = CatalogPortProfiles.CAPABILITIES_PORT_ID;
    public static final String AGENT_PLUG_PORT_ID = CatalogPortProfiles.AGENT_PLUG_PORT_ID;

    public static final String MESSAGE_COLOR = CatalogPortProfiles.MESSAGE_COLOR;
    public static final String CAPABILITIES_COLOR = CatalogPortProfiles.CAPABILITIES_COLOR;
    public static final String AGENT_PLUG_COLOR = CatalogPortProfiles.AGENT_PLUG_COLOR;
    public static final String PLANNER_ROUTED_MESSAGE_COLOR = CatalogPortProfiles.PLANNER_ROUTED_MESSAGE_COLOR;

    private static final Set<String> KNOWN_WIRE_TYPES =
            Set.of("any", MESSAGE_WIRE, CAPABILITIES_WIRE, AGENT_PLUG_WIRE);

    private CatalogPortPopulator() {
    }

    public static List<PortDescriptor> resolveInputs(OloPort[] ports, OloCanvasPorts profile) {
        if (ports != null && ports.length > 0) {
            return materializePorts(ports, false);
        }
        return CatalogPortProfiles.profileInputs(profile);
    }

    public static List<PortDescriptor> resolveOutputs(OloPort[] ports, OloCanvasPorts profile) {
        if (ports != null && ports.length > 0) {
            return materializePorts(ports, true);
        }
        return CatalogPortProfiles.profileOutputs(profile);
    }

    private static List<PortDescriptor> materializePorts(OloPort[] ports, boolean output) {
        List<PortDescriptor> out = new ArrayList<>();
        for (OloPort port : ports) {
            out.add(materializePort(port, output));
        }
        return out;
    }

    public static PortDescriptor materializePort(OloPort port, boolean output) {
        PortDescriptor descriptor = new PortDescriptor();
        descriptor.id = port.id();
        descriptor.name = CatalogDefaults.materializePortName(port.id(), port.name());
        descriptor.label = descriptor.name;
        descriptor.schema = port.schema();
        descriptor.required = port.required();
        descriptor.minConnections = port.minConnections();
        int maxConnections = port.maxConnections();
        descriptor.maxConnections = maxConnections < 0 ? null : maxConnections;
        descriptor.description = CatalogDefaults.blankToNull(port.description());
        descriptor.ui = portUi(port, output);
        applyWireMetadata(descriptor, output);
        return descriptor;
    }

    private static void applyWireMetadata(PortDescriptor descriptor, boolean output) {
        String wire = canonicalWire(descriptor.schema);
        if (!KNOWN_WIRE_TYPES.contains(wire)) {
            return;
        }
        descriptor.type = wire;
        descriptor.label = switch (wire) {
            case MESSAGE_WIRE -> output ? "message out" : "message in";
            case CAPABILITIES_WIRE -> "available tools";
            case AGENT_PLUG_WIRE -> "available agents";
            default -> descriptor.label;
        };
        if (!output) {
            descriptor.acceptType = wire;
        }
        if (descriptor.ui == null) {
            descriptor.ui = new PortUiDescriptor();
        }
        if (descriptor.ui.color == null) {
            descriptor.ui.color = switch (wire) {
                case CAPABILITIES_WIRE -> CAPABILITIES_COLOR;
                case AGENT_PLUG_WIRE -> AGENT_PLUG_COLOR;
                default -> MESSAGE_COLOR;
            };
        }
    }

    private static PortUiDescriptor portUi(OloPort port, boolean output) {
        PortUiDescriptor ui = new PortUiDescriptor();
        ui.position = resolvePortPosition(port.position(), output).name();
        String wire = canonicalWire(port.schema());
        ui.color = switch (wire) {
            case CAPABILITIES_WIRE -> CAPABILITIES_COLOR;
            case AGENT_PLUG_WIRE -> AGENT_PLUG_COLOR;
            default -> MESSAGE_COLOR;
        };
        return ui;
    }

    private static OloPortPosition resolvePortPosition(OloPortPosition position, boolean output) {
        if (position != null && position != OloPortPosition.DEFAULT) {
            return position;
        }
        return output ? OloPortPosition.RIGHT : OloPortPosition.LEFT;
    }

    private static String canonicalWire(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
