/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowValidatorEdgeTest {

    @Test
    void rejectsUnknownEdgeEndpoints() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .capability(ValidationTestFixtures.minimalCapability())
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown source node"));
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown target node"));
    }

    @Test
    void acceptsCompatibleTypedPorts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typed-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("tool-a", NodeType.TOOL)
                        .addPort(PortDefinition.outputPort("stockList", "Stock[]"))
                        .build())
                .addNode(ValidationTestFixtures.node("tool-b", NodeType.TOOL)
                        .addPort(PortDefinition.inputPort("stocks", "Stock[]"))
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePortId("stockList")
                        .targetNodeId("tool-b")
                        .targetPortId("stocks")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsIncompatibleTypedPorts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typed-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("tool-a", NodeType.TOOL)
                        .addPort(PortDefinition.outputPort("result", "String"))
                        .build())
                .addNode(ValidationTestFixtures.node("tool-b", NodeType.TOOL)
                        .addPort(PortDefinition.inputPort("stocks", "Stock[]"))
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePortId("result")
                        .targetNodeId("tool-b")
                        .targetPortId("stocks")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("wire type mismatch"));
        assertThat(result.errors()).anyMatch(e -> e.contains("String"));
        assertThat(result.errors()).anyMatch(e -> e.contains("Stock[]"));
    }

    @Test
    void rejectsUnknownOutputPort() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("unknown-port")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("tool-a", NodeType.TOOL)
                        .addPort(PortDefinition.outputPort("out", "String"))
                        .build())
                .addNode(ValidationTestFixtures.node("tool-b", NodeType.TOOL).build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePortId("missing")
                        .targetNodeId("tool-b")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown output port"));
    }

    @Test
    void requiresPortNameWhenMultipleOutputsDeclared() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("multi-out")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("router", NodeType.CONDITION)
                        .addPort(PortDefinition.outputPort("true", "any"))
                        .addPort(PortDefinition.outputPort("false", "any"))
                        .build())
                .addNode(ValidationTestFixtures.node("sink", NodeType.END).build())
                .addEdge(EdgeDefinition.builder().sourceNodeId("router").targetNodeId("sink").build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("sourcePortId/targetPortId is required"));
    }
}
