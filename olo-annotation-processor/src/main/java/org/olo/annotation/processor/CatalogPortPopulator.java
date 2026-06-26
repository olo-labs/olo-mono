package org.olo.annotation.processor;

import org.olo.annotation.OloCanvasPorts;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloPortPosition;
import org.olo.annotation.catalog.PortDescriptor;
import org.olo.annotation.catalog.PortUiDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Materializes {@link OloPort} and {@link OloCanvasPorts} profiles for catalog export. */
final class CatalogPortPopulator {

    static final String MESSAGE_WIRE = "message";
    static final String CAPABILITIES_WIRE = "capabilities";
    static final String AGENT_PLUG_WIRE = "agent-plug";

    static final String CAPABILITIES_PORT_ID = "capabilities";
    static final String AGENT_PLUG_PORT_ID = "agentPlug";

    static final String MESSAGE_COLOR = "#ef4444";
    static final String CAPABILITIES_COLOR = "#22c55e";
    static final String AGENT_PLUG_COLOR = "#a855f7";
    static final String PLANNER_ROUTED_MESSAGE_COLOR = "#71717a";

    private static final Set<String> KNOWN_WIRE_TYPES =
            Set.of("any", MESSAGE_WIRE, CAPABILITIES_WIRE, AGENT_PLUG_WIRE);

    private CatalogPortPopulator() {
    }

    static List<PortDescriptor> resolveInputs(OloPort[] ports, OloCanvasPorts profile) {
        if (ports != null && ports.length > 0) {
            return materializePorts(ports, false);
        }
        return profileInputs(profile);
    }

    static List<PortDescriptor> resolveOutputs(OloPort[] ports, OloCanvasPorts profile) {
        if (ports != null && ports.length > 0) {
            return materializePorts(ports, true);
        }
        return profileOutputs(profile);
    }

    static List<PortDescriptor> profileInputs(OloCanvasPorts profile) {
        return switch (profile) {
            case CAPABILITY_PLUGIN, AGENT_PLUGIN -> List.of(plannerRoutedMessageInput());
            case PLANNER_HOST -> List.of(messageInput(), capabilitiesInput(), agentPlugInput());
            case NONE -> List.of();
        };
    }

    static List<PortDescriptor> profileOutputs(OloCanvasPorts profile) {
        return switch (profile) {
            case CAPABILITY_PLUGIN -> List.of(plannerRoutedMessageOutput(), capabilitiesOutput());
            case AGENT_PLUGIN -> List.of(plannerRoutedMessageOutput(), agentPlugOutput());
            case PLANNER_HOST -> List.of(messageOutput());
            case NONE -> List.of();
        };
    }

    private static List<PortDescriptor> materializePorts(OloPort[] ports, boolean output) {
        List<PortDescriptor> out = new ArrayList<>();
        for (OloPort port : ports) {
            out.add(materializePort(port, output));
        }
        return out;
    }

    static PortDescriptor materializePort(OloPort port, boolean output) {
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

    private static PortDescriptor messageInput() {
        PortDescriptor port = new PortDescriptor();
        port.id = "in";
        port.name = "in";
        port.label = "message in";
        port.schema = MESSAGE_WIRE;
        port.type = MESSAGE_WIRE;
        port.acceptType = MESSAGE_WIRE;
        port.required = true;
        port.minConnections = 1;
        port.maxConnections = 1;
        port.description = "Incoming workflow message";
        port.ui = ui(OloPortPosition.LEFT, MESSAGE_COLOR);
        return port;
    }

    private static PortDescriptor messageOutput() {
        PortDescriptor port = new PortDescriptor();
        port.id = "out";
        port.name = "out";
        port.label = "message out";
        port.schema = MESSAGE_WIRE;
        port.type = MESSAGE_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Outgoing workflow message";
        port.ui = ui(OloPortPosition.RIGHT, MESSAGE_COLOR);
        return port;
    }

    private static PortDescriptor plannerRoutedMessageInput() {
        PortDescriptor port = new PortDescriptor();
        port.id = "in";
        port.name = "in";
        port.label = "message in";
        port.schema = MESSAGE_WIRE;
        port.type = MESSAGE_WIRE;
        port.acceptType = MESSAGE_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Incoming message (routed by planner/agent at runtime)";
        port.ui = ui(OloPortPosition.LEFT, PLANNER_ROUTED_MESSAGE_COLOR);
        return port;
    }

    private static PortDescriptor plannerRoutedMessageOutput() {
        PortDescriptor port = new PortDescriptor();
        port.id = "out";
        port.name = "out";
        port.label = "message out";
        port.schema = MESSAGE_WIRE;
        port.type = MESSAGE_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Outgoing message (routed by planner/agent at runtime)";
        port.ui = ui(OloPortPosition.RIGHT, PLANNER_ROUTED_MESSAGE_COLOR);
        return port;
    }

    private static PortDescriptor capabilitiesInput() {
        PortDescriptor port = new PortDescriptor();
        port.id = CAPABILITIES_PORT_ID;
        port.name = CAPABILITIES_PORT_ID;
        port.label = "available tools";
        port.schema = CAPABILITIES_WIRE;
        port.type = CAPABILITIES_WIRE;
        port.acceptType = CAPABILITIES_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Tools and hooks registered for runtime prompt assembly (0 or more)";
        port.ui = ui(OloPortPosition.BOTTOM, CAPABILITIES_COLOR);
        return port;
    }

    private static PortDescriptor agentPlugInput() {
        PortDescriptor port = new PortDescriptor();
        port.id = AGENT_PLUG_PORT_ID;
        port.name = AGENT_PLUG_PORT_ID;
        port.label = "agent plug";
        port.schema = AGENT_PLUG_WIRE;
        port.type = AGENT_PLUG_WIRE;
        port.acceptType = AGENT_PLUG_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Child workflows registered for runtime prompt assembly (0 or more)";
        port.ui = ui(OloPortPosition.BOTTOM, AGENT_PLUG_COLOR);
        return port;
    }

    private static PortDescriptor capabilitiesOutput() {
        PortDescriptor port = new PortDescriptor();
        port.id = CAPABILITIES_PORT_ID;
        port.name = CAPABILITIES_PORT_ID;
        port.label = "available tools";
        port.schema = CAPABILITIES_WIRE;
        port.type = CAPABILITIES_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Capability indicator for runtime prompt assembly on a connected agent";
        port.ui = ui(OloPortPosition.TOP, CAPABILITIES_COLOR);
        return port;
    }

    private static PortDescriptor agentPlugOutput() {
        PortDescriptor port = new PortDescriptor();
        port.id = AGENT_PLUG_PORT_ID;
        port.name = AGENT_PLUG_PORT_ID;
        port.label = "agent plug";
        port.schema = AGENT_PLUG_WIRE;
        port.type = AGENT_PLUG_WIRE;
        port.required = false;
        port.minConnections = 0;
        port.description = "Child workflow indicator for runtime prompt assembly on a connected agent";
        port.ui = ui(OloPortPosition.TOP, AGENT_PLUG_COLOR);
        return port;
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

    private static PortUiDescriptor ui(OloPortPosition position, String color) {
        PortUiDescriptor ui = new PortUiDescriptor();
        ui.position = position.name();
        ui.color = color;
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
