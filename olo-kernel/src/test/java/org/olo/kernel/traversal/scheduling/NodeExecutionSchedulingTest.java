package org.olo.kernel.traversal.scheduling;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutionSchedulingTest {

    @Test
    void inlinePlannerWithActivityKindUsesDedicatedActivity() {
        NodeDefinition planner = NodeDefinition.builder()
                .id("graph-planner")
                .type("AGENT")
                .executionModel(ExecutionModel.INLINE)
                .executionKind(ExecutionKind.ACTIVITY)
                .build();

        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(planner)).isTrue();
        assertThat(NodeExecutionScheduling.runsInWorkflow(planner)).isFalse();
    }

    @Test
    void inlineWithoutActivityKindRunsInWorkflow() {
        NodeDefinition router = NodeDefinition.builder()
                .id("route")
                .type("ROUTER")
                .executionModel(ExecutionModel.INLINE)
                .build();

        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(router)).isFalse();
        assertThat(NodeExecutionScheduling.runsInWorkflow(router)).isTrue();
    }

    @Test
    void nodesWithoutExecutionMetadataUseDedicatedActivities() {
        NodeDefinition start = NodeDefinition.builder().id("start").type("START").build();
        NodeDefinition tool = NodeDefinition.builder().id("read-logs").type("TOOL").build();

        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(start)).isTrue();
        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(tool)).isTrue();
    }
}
