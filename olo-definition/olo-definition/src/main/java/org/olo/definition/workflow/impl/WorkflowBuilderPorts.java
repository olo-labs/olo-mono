/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDefinitionBuilder;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortUiDefinition;
import org.olo.definition.port.PortUiPosition;
import org.olo.definition.port.PortWireType;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.Map;

/**
 * Studio port wiring factories shared by {@link WorkflowBuilderNodes} and {@link WorkflowBuilderCanvas}.
 */
public final class WorkflowBuilderPorts {

    private static final String MESSAGE_PORT_COLOR = "#ef4444";
    private static final String CAPABILITIES_PORT_COLOR = "#22c55e";
    private static final String AGENT_PLUG_PORT_COLOR = "#a855f7";
    private static final String MESSAGE_INPUT_PORT_LABEL = "message in";
    private static final String MESSAGE_OUTPUT_PORT_LABEL = "message out";
    private static final String MESSAGE_INPUT_PORT_DESCRIPTION = "Incoming workflow message";
    private static final String MESSAGE_OUTPUT_PORT_DESCRIPTION = "Outgoing workflow message";

    private WorkflowBuilderPorts() {
    }

    /** Message wire port used by Studio presets (labels, color, cardinality). */
    public static PortDefinition messagePort(String id, PortDirection direction) {
        return defaultPort(id, id, direction);
    }

    /** Capabilities plugin port for tool wiring on agent nodes. */
    public static PortDefinition capabilitiesPort(PortDirection direction) {
        return pluginPort("capabilities", PortWireType.CAPABILITIES, direction);
    }

    /** Agent-plug port for child-workflow wiring on agent nodes. */
    public static PortDefinition agentPlugPort(PortDirection direction) {
        return pluginPort("agentPlug", PortWireType.AGENT_PLUG, direction);
    }

    static PortDefinition optionalMessagePort(String id, PortDirection direction) {
        String label = direction == PortDirection.INPUT ? MESSAGE_INPUT_PORT_LABEL : MESSAGE_OUTPUT_PORT_LABEL;
        String wireType = PortWireType.MESSAGE.wireName();
        PortDefinitionBuilder builder = PortDefinition.builder()
                .id(id)
                .label(label)
                .shortDescription(direction == PortDirection.INPUT
                        ? MESSAGE_INPUT_PORT_DESCRIPTION
                        : MESSAGE_OUTPUT_PORT_DESCRIPTION)
                .schema(wireType)
                .type(wireType)
                .direction(direction)
                .ui(PortUiDefinition.builder()
                        .position(PortUiPosition.defaultFor(direction))
                        .color(MESSAGE_PORT_COLOR)
                        .build())
                .required(false)
                .minConnections(0);
        if (direction == PortDirection.INPUT) {
            builder.acceptType(wireType);
        }
        return builder.build();
    }

    static PortDefinition defaultPort(String id, String name, PortDirection direction) {
        String label = direction == PortDirection.INPUT ? MESSAGE_INPUT_PORT_LABEL : MESSAGE_OUTPUT_PORT_LABEL;
        String wireType = PortWireType.MESSAGE.wireName();
        PortDefinitionBuilder builder = PortDefinition.builder()
                .id(id)
                .label(label)
                .shortDescription(direction == PortDirection.INPUT
                        ? MESSAGE_INPUT_PORT_DESCRIPTION
                        : MESSAGE_OUTPUT_PORT_DESCRIPTION)
                .schema(wireType)
                .type(wireType)
                .direction(direction)
                .ui(PortUiDefinition.builder()
                        .position(PortUiPosition.defaultFor(direction))
                        .color(MESSAGE_PORT_COLOR)
                        .build());
        if (direction == PortDirection.INPUT) {
            builder.acceptType(wireType)
                    .required(true)
                    .minConnections(1)
                    .maxConnections(1);
        } else {
            builder.required(false).minConnections(0);
        }
        return builder.build();
    }

    static PortDefinition pluginPort(String id, PortWireType wireType, PortDirection direction) {
        PortUiPosition position = direction == PortDirection.INPUT ? PortUiPosition.BOTTOM : PortUiPosition.TOP;
        String color = wireType == PortWireType.CAPABILITIES ? CAPABILITIES_PORT_COLOR : AGENT_PLUG_PORT_COLOR;
        String label = wireType == PortWireType.CAPABILITIES ? "available tools" : "available agents";
        String description = direction == PortDirection.INPUT
                ? (wireType == PortWireType.CAPABILITIES
                        ? "Tools and hooks registered for runtime prompt assembly (0 or more)"
                        : "Child workflows registered for runtime prompt assembly (0 or more)")
                : (wireType == PortWireType.CAPABILITIES
                        ? "Capability indicator for runtime prompt assembly on a connected agent"
                        : "Child workflow indicator for runtime prompt assembly on a connected agent");
        PortDefinitionBuilder builder = PortDefinition.builder()
                .id(id)
                .label(label)
                .shortDescription(description)
                .schema(wireType.wireName())
                .type(wireType.wireName())
                .direction(direction)
                .ui(PortUiDefinition.builder().position(position).color(color).build())
                .required(false)
                .minConnections(0);
        if (direction == PortDirection.INPUT) {
            builder.acceptType(wireType.wireName());
        }
        return builder.build();
    }

    static NodeDefinition nodeWithConfiguration(NodeDefinition node, Map<String, Object> configuration) {
        NodeDefinitionBuilder builder = NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(node.getLabel())
                .capability(node.getCapability())
                .ports(node.getPorts())
                .reads(node.getReads())
                .writes(node.getWrites())
                .configuration(configuration)
                .hooks(node.getHooks());
        if (node.getExecution() != null) {
            builder.execution(node.getExecution());
        }
        return builder.build();
    }
}
