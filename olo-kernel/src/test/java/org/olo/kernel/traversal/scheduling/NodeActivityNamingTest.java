package org.olo.kernel.traversal.scheduling;

import org.junit.jupiter.api.Test;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeActivityNamingTest {

    @Test
    void formatsWorkflowFromIdAndLabel() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("dynamic-graph-creation")
                .label("Dynamic Graph Creation")
                .build();

        assertThat(NodeActivityNaming.formatWorkflow(graph))
                .isEqualTo("dynamic-graph-creation:Dynamic Graph Creation");
    }

    @Test
    void formatsQueueAsIdLabel() {
        assertThat(NodeActivityNaming.formatQueue("dynamic-graph-creation"))
                .isEqualTo("dynamic-graph-creation:Dynamic Graph Creation");
    }

    @Test
    void formatsNodeUsingCapabilityName() {
        NodeDefinition node = NodeDefinition.builder()
                .id("graph-planner")
                .type("AGENT")
                .capability(CapabilityDefinition.builder()
                        .name("Graph Planner")
                        .description("Plans workflow graphs")
                        .build())
                .build();

        assertThat(NodeActivityNaming.formatNode(node)).isEqualTo("graph-planner:Graph Planner");
    }

    @Test
    void formatsToolNodeUsingToolId() {
        NodeDefinition node = NodeDefinition.builder()
                .id("read-logs")
                .type("TOOL")
                .configuration(Map.of("toolId", "olo-core:log-reader"))
                .build();

        assertThat(NodeActivityNaming.formatNode(node)).isEqualTo("read-logs:Log Reader");
    }
}
