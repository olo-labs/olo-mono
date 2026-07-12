/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class AvailableAgentsJsonResolverTest {

    @Test
    void resolvesOnlyAgentPlugConnectedChildAgents() {
        WorkflowDefinition orchestrator = WorkflowBuilder.create("Orchestrator")
                .id("orchestrator")
                .queue("test")
                .canvasChildAgentPluginNode("literature-agent", "literature-agent", "Literature Agent")
                .connect("literature-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .build();

        NodeDefinition plugin = orchestrator.getNodes().stream()
                .filter(node -> "literature-agent".equals(node.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(plugin.getConfiguration()).containsEntry("delegateAgentId", "literature-agent");

        assertThat(AvailableAgentsJsonResolver.resolveAgentIds(
                        orchestrator, ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID))
                .containsExactly("literature-agent");

        String json = AvailableAgentsJsonResolver.resolve(
                orchestrator, ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID);
        assertThat(json).contains("literature-agent");
        assertThat(json).contains("Literature Agent");
    }

    @Test
    void rejectsAgentsNotConnectedOnCanvas() {
        WorkflowDefinition orchestrator = WorkflowBuilder.create("Orchestrator")
                .id("orchestrator")
                .queue("test")
                .canvasChildAgentPluginNode("literature-agent", "literature-agent", "Literature Agent")
                .connect("literature-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .build();

        assertThat(AvailableAgentsJsonResolver.isAllowedAgent(
                        orchestrator,
                        ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID,
                        "literature-agent"))
                .isTrue();
        assertThat(AvailableAgentsJsonResolver.isAllowedAgent(
                        orchestrator, ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "unknown-agent"))
                .isFalse();
    }
}
