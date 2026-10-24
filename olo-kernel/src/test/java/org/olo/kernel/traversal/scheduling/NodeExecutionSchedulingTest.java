/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.scheduling;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutionSchedulingTest {

    @Test
    void inlineLlmAndToolEffectsAlwaysHaveAnActivityBoundary() {
        for (String type : java.util.List.of("AGENT","TOOL")) {
            var node = NodeDefinition.builder().id("effect").type(type).executionModel(ExecutionModel.INLINE).build();
            assertThat(NodeExecutionScheduling.requiresDedicatedActivity(node)).isTrue();
        }
        var dispatch=NodeDefinition.builder().id("children").type("AGENT").executionModel(ExecutionModel.INLINE)
            .configuration(java.util.Map.of("agentCallDispatch",true)).build();
        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(dispatch)).isFalse();
    }

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
    void childWorkflowRunsInWorkflowThread() {
        NodeDefinition childAgent = NodeDefinition.builder()
                .id("delegate")
                .workflow(org.olo.definition.workflow.WorkflowReferenceDefinition.builder().workflowId("child").build())
                .type("AGENT")
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .build();

        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(childAgent)).isFalse();
        assertThat(NodeExecutionScheduling.runsInWorkflow(childAgent)).isTrue();
    }

    @Test
    void nodesWithoutExecutionMetadataUseDedicatedActivities() {
        NodeDefinition start = NodeDefinition.builder().id("start").type("START").build();
        NodeDefinition tool = NodeDefinition.builder().id("read-logs").type("TOOL").build();

        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(start)).isTrue();
        assertThat(NodeExecutionScheduling.requiresDedicatedActivity(tool)).isTrue();
    }
}
