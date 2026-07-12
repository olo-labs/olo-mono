/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.catalog;

import org.olo.annotation.OloCanvasPorts;
import org.olo.annotation.OloPortPosition;
import org.olo.annotation.catalog.PortDescriptor;
import org.olo.annotation.catalog.PortUiDescriptor;

/**
 * Predefined canvas port profiles for tools and hooks that use {@link OloCanvasPorts} instead of
 * explicit {@link org.olo.annotation.OloPort} declarations.
 */
public final class CatalogPortProfiles {

    public static final String MESSAGE_WIRE = "message";
    public static final String CAPABILITIES_WIRE = "capabilities";
    public static final String AGENT_PLUG_WIRE = "agent-plug";

    public static final String CAPABILITIES_PORT_ID = "capabilities";
    public static final String AGENT_PLUG_PORT_ID = "agentPlug";

    public static final String MESSAGE_COLOR = "#ef4444";
    public static final String CAPABILITIES_COLOR = "#22c55e";
    public static final String AGENT_PLUG_COLOR = "#a855f7";
    public static final String PLANNER_ROUTED_MESSAGE_COLOR = "#71717a";

    private CatalogPortProfiles() {
    }

    public static java.util.List<PortDescriptor> profileInputs(OloCanvasPorts profile) {
        return switch (profile) {
            case CAPABILITY_PLUGIN, AGENT_PLUGIN -> java.util.List.of(plannerRoutedMessageInput());
            case PLANNER_HOST -> java.util.List.of(messageInput(), capabilitiesInput(), agentPlugInput());
            case NONE -> java.util.List.of();
        };
    }

    public static java.util.List<PortDescriptor> profileOutputs(OloCanvasPorts profile) {
        return switch (profile) {
            case CAPABILITY_PLUGIN -> java.util.List.of(plannerRoutedMessageOutput(), capabilitiesOutput());
            case AGENT_PLUGIN -> java.util.List.of(plannerRoutedMessageOutput(), agentPlugOutput());
            case PLANNER_HOST -> java.util.List.of(messageOutput());
            case NONE -> java.util.List.of();
        };
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

    private static PortUiDescriptor ui(OloPortPosition position, String color) {
        PortUiDescriptor ui = new PortUiDescriptor();
        ui.position = position.name();
        ui.color = color;
        return ui;
    }
}
