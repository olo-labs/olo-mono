/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.graph.index;

import org.junit.jupiter.api.Test;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultGraphIndexTest {

    @Test
    void indexesNodesAndOutgoingEdges() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("demo")
                .nodes(List.of(
                        NodeDefinition.builder().id("start").type("START").build(),
                        NodeDefinition.builder().id("agent").type("AGENT").build(),
                        NodeDefinition.builder().id("end").type("END").build()))
                .edges(List.of(
                        EdgeDefinition.builder()
                                .sourceNodeId("start")
                                .sourcePortId("out")
                                .targetNodeId("agent")
                                .targetPortId("in")
                                .build(),
                        EdgeDefinition.builder()
                                .sourceNodeId("agent")
                                .sourcePortId("out")
                                .targetNodeId("end")
                                .targetPortId("in")
                                .build()))
                .build();

        GraphIndex index = new DefaultGraphIndex(graph);

        assertThat(index.findNode("agent")).isPresent();
        assertThat(index.outgoingEdges("start")).hasSize(1);
        assertThat(index.outgoingEdges("start").getFirst().getTargetNodeId()).isEqualTo("agent");
    }
}
