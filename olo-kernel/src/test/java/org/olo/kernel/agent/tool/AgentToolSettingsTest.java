/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.agentcall.AgentCallPlannerSupport;
import org.olo.kernel.exception.KernelException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;

class AgentToolSettingsTest {

    @Test
    void primaryAgentDefaultsToTheWorkflowId() {
        NodeDefinition agent = agentNode("agent", Map.of());
        NodeDefinition child = agentNode("metrics-rca-agent", Map.of());

        assertThat(AgentToolSettings.resolve(graph("log-failure-agent", agent, child), agent, 8).agentName())
                .isEqualTo("log-failure-agent");
    }

    @Test
    void onlyAgentNodeDefaultsToTheWorkflowId() {
        NodeDefinition triage = agentNode("triage", Map.of());

        assertThat(AgentToolSettings.resolve(graph("triage-agent", triage), triage, 8).agentName())
                .isEqualTo("triage-agent");
    }

    @Test
    void otherAgentNodesDefaultToTheirNodeId() {
        NodeDefinition agent = agentNode("agent", Map.of());
        NodeDefinition child = agentNode("metrics-rca-agent", Map.of());

        assertThat(AgentToolSettings.resolve(graph("log-rca-orchestrator", agent, child), child, 8).agentName())
                .isEqualTo("metrics-rca-agent");
    }

    @Test
    void userCanOverrideTheAgentName() {
        NodeDefinition agent = agentNode("agent", Map.of("agentName", " oncall-readonly "));

        assertThat(AgentToolSettings.resolve(graph("triage-agent", agent), agent, 8).agentName())
                .isEqualTo("oncall-readonly");
    }

    @Test
    void rejectsAgentNamesThatAreNotSafeIdentities() {
        NodeDefinition agent = agentNode("agent", Map.of("agentName", "../admin"));

        assertThatThrownBy(() -> AgentToolSettings.resolve(graph("triage-agent", agent), agent, 8))
                .isInstanceOf(KernelException.class)
                .hasMessageContaining("invalid agentName");
    }

    @Test
    void maxToolRoundsFromNodeOrWorkerDefault() {
        NodeDefinition fromNode = agentNode("agent", Map.of("maxToolRounds", 3));
        NodeDefinition fromText = agentNode("agent", Map.of("maxToolRounds", "0"));
        NodeDefinition fromDefault = agentNode("agent", Map.of());

        assertThat(AgentToolSettings.resolve(graph("w", fromNode), fromNode, 8).maxToolRounds()).isEqualTo(3);
        assertThat(AgentToolSettings.resolve(graph("w", fromText), fromText, 8).maxToolRounds()).isZero();
        assertThat(AgentToolSettings.resolve(graph("w", fromDefault), fromDefault, 8).maxToolRounds()).isEqualTo(8);
    }

    @Test
    void rejectsBadMaxToolRounds() {
        NodeDefinition negative = agentNode("agent", Map.of("maxToolRounds", -1));
        NodeDefinition text = agentNode("agent", Map.of("maxToolRounds", "many"));

        assertThatThrownBy(() -> AgentToolSettings.resolve(graph("w", negative), negative, 8)).isInstanceOf(KernelException.class);
        assertThatThrownBy(() -> AgentToolSettings.resolve(graph("w", text), text, 8)).isInstanceOf(KernelException.class);
    }

    @Test
    void plannerAgentsDoNotGetTools() {
        NodeDefinition planner = agentNode("agent", Map.of(AgentCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true));

        assertThat(AgentToolSettings.usesTools(planner)).isFalse();
        assertThat(AgentToolSettings.usesTools(agentNode("agent", Map.of()))).isTrue();
    }

    @Test
    void toolNamesAreMadeSafeForFunctionCallingAndMappedBack() {
        var mapper = new ToolNameMapper(java.util.List.of("incidents.host-status", "incidents_host-status", "a/b"));

        assertThat(mapper.modelName("incidents.host-status")).isEqualTo("incidents_host-status");
        assertThat(mapper.modelName("incidents_host-status")).isEqualTo("incidents_host-status_2");
        assertThat(mapper.modelName("a/b")).isEqualTo("a_b");
        assertThat(mapper.toolGateName("incidents_host-status")).contains("incidents.host-status");
        assertThat(mapper.toolGateName("incidents_host-status_2")).contains("incidents_host-status");
        assertThat(mapper.toolGateName("incidents.host-status")).contains("incidents.host-status");
        assertThat(mapper.toolGateName("rm-rf")).isEmpty();
    }
}
