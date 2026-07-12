/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeDefinitionBuilder;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

/** Shared builders used by programmatic sample workflow definitions. */
final class SampleWorkflowBuilders {

    private SampleWorkflowBuilders() {
    }

    static CapabilityDefinition passThroughCapability(String name, String description) {
        return CapabilityDefinition.builder()
                .name(name)
                .description(description)
                .addInput("input")
                .addOutput("output")
                .build();
    }

    static NodeDefinitionBuilder nodeWithDefaultPorts(String id, NodeType type) {
        NodeDefinitionBuilder builder = NodeDefinition.builder().id(id).type(type);
        if (type != NodeType.START) {
            builder.addPort(WorkflowBuilder.messagePort("in", PortDirection.INPUT));
        }
        if (type != NodeType.END) {
            builder.addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT));
        }
        return builder;
    }

    /** For nodes only reached via error routes (no incoming graph edges). */
    static NodeDefinitionBuilder nodeWithOptionalInputPorts(String id, NodeType type) {
        NodeDefinitionBuilder builder = NodeDefinition.builder().id(id).type(type);
        if (type != NodeType.START) {
            String wireType = PortWireType.MESSAGE.wireName();
            builder.addPort(PortDefinition.builder()
                    .id("in")
                    .label("message in")
                    .schema(wireType)
                    .type(wireType)
                    .acceptType(wireType)
                    .direction(PortDirection.INPUT)
                    .required(false)
                    .minConnections(0)
                    .build());
        }
        if (type != NodeType.END) {
            builder.addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT));
        }
        return builder;
    }

    static PortDefinition messageBranchPort(String id) {
        String wireType = PortWireType.MESSAGE.wireName();
        return PortDefinition.builder()
                .id(id)
                .label(id)
                .schema(wireType)
                .type(wireType)
                .direction(PortDirection.OUTPUT)
                .required(false)
                .minConnections(0)
                .build();
    }

    /** END node that accepts multiple converging branches (e.g. conditional routing). */
    static NodeDefinition multiInputOutputNode(String id) {
        String wireType = PortWireType.MESSAGE.wireName();
        return NodeDefinition.builder()
                .id(id)
                .type(NodeType.END)
                .addPort(PortDefinition.builder()
                        .id("in")
                        .label("message in")
                        .schema(wireType)
                        .type(wireType)
                        .acceptType(wireType)
                        .direction(PortDirection.INPUT)
                        .required(true)
                        .minConnections(1)
                        .build())
                .build();
    }

    /** MODEL node that accepts multiple converging parallel branches. */
    static NodeDefinition multiInputModelNode(String id, String subtype) {
        String wireType = PortWireType.MESSAGE.wireName();
        NodeDefinitionBuilder builder = NodeDefinition.builder()
                .id(id)
                .type(NodeType.MODEL)
                .addPort(PortDefinition.builder()
                        .id("in")
                        .label("message in")
                        .schema(wireType)
                        .type(wireType)
                        .acceptType(wireType)
                        .direction(PortDirection.INPUT)
                        .required(true)
                        .minConnections(1)
                        .build())
                .addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT));
        if (subtype != null) {
            builder.subtype(subtype);
        }
        return builder.build();
    }

    static WorkflowDefinition buildSample(WorkflowBuilder builder) {
        return builder.withStandardReturnVariable().build();
    }
}
