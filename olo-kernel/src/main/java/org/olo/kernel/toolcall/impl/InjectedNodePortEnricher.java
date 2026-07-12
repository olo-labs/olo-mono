/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;

public final class InjectedNodePortEnricher {

    private InjectedNodePortEnricher() {
    }

    public static NodeDefinition withDefaultPorts(NodeDefinition node) {
        if (node.getPorts() != null && !node.getPorts().isEmpty()) {
            return node;
        }
        NodeDefinitionBuilder builder = NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(node.getLabel())
                .execution(node.getExecution())
                .configuration(node.getConfiguration());
        if (!NodeType.END.name().equals(node.getType())) {
            builder.addPort(PortDefinition.builder()
                    .id("out")
                    .name("out")
                    .schema("any")
                    .direction(PortDirection.OUTPUT)
                    .build());
        }
        if (!NodeType.START.name().equals(node.getType())) {
            builder.addPort(PortDefinition.builder()
                    .id("in")
                    .name("in")
                    .schema("any")
                    .direction(PortDirection.INPUT)
                    .build());
        }
        return builder.build();
    }
}
