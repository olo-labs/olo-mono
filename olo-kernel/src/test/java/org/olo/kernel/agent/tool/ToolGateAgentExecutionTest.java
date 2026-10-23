/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agent.executor.impl.LocalLlmAgentExecutor;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.agent.prompt.PromptRenderer;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.MODEL;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.answer;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.callTool;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.context;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.tool;

/**
 * AGENT node execution end to end through {@link LocalLlmAgentExecutor} and {@link DefaultLlmInvocationService}.
 */
class ToolGateAgentExecutionTest {

    private static final PromptRenderer PROMPT = new PromptRenderer() {
        @Override
        public String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables) {
            return "prompt";
        }

        @Override
        public String renderForNode(WorkflowDefinition graph, NodeDefinition node, WorkflowRuntimeVariables variables) {
            return "prompt for " + node.getId();
        }
    };

    private LocalLlmAgentExecutor executor(AgentToolTestSupport.ScriptedLlm llm, AtomicReference<AgentToolAccess> access) {
        return new LocalLlmAgentExecutor(new DefaultLlmInvocationService(PROMPT, graph -> MODEL, llm, access::get));
    }

    @Test
    void withoutToolGateAgentsRunExactlyAsBefore() {
        var llm = new AgentToolTestSupport.ScriptedLlm();
        NodeDefinition node = agentNode("agent", Map.of());
        WorkflowDefinition graph = graph("triage-agent", node);

        NodeResult result = executor(llm, new AtomicReference<>(AgentToolAccess.disabled()))
                .execute(context(graph, "run-1"), node);

        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(llm.completions).containsExactly("prompt for agent");
        assertThat(result.output()).doesNotContainKeys("agentName", "toolInvocations");
    }

    @Test
    void eachAgentUsesItsOwnIdentityAndRecordsItsToolCalls() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of(
                "triage-agent", List.of(tool("incidents.host-status")),
                "incident-report-agent", List.of(tool("incidents.update-incident"))));
        var access = new AtomicReference<>(AgentToolAccess.enabled(toolGate, 8));

        var triageLlm = new AgentToolTestSupport.ScriptedLlm().reply(
                callTool("incidents_host-status", "{\"host\":\"app-02\"}"), answer("OOM on app-02"));
        NodeDefinition triage = agentNode("agent", Map.of());
        NodeResult triageResult = executor(triageLlm, access)
                .execute(context(graph("triage-agent", triage), "run-1"), triage);

        var reportLlm = new AgentToolTestSupport.ScriptedLlm().reply(answer("nothing to update"));
        NodeDefinition report = agentNode("agent", Map.of("agentName", "incident-report-agent"));
        NodeResult reportResult = executor(reportLlm, access)
                .execute(context(graph("triage-agent", report), "run-1"), report);

        assertThat(toolGate.listedFor).containsExactly("triage-agent", "incident-report-agent");
        assertThat(triageLlm.chatTools.get(0)).extracting(s -> s.name()).containsExactly("incidents_host-status");
        assertThat(reportLlm.chatTools.get(0)).extracting(s -> s.name()).containsExactly("incidents_update-incident");

        assertThat(triageResult.output())
                .containsEntry("agentName", "triage-agent")
                .containsEntry("agentOutcome", "COMPLETED")
                .containsEntry("response", "OOM on app-02")
                .containsEntry("toolCatalog", List.of("incidents.host-status"));
        assertThat((List<?>) triageResult.output().get("toolInvocations")).hasSize(1);
        assertThat(reportResult.output()).containsEntry("agentName", "incident-report-agent");
    }

    @Test
    void plannerAgentsKeepAnsweringWithAPlan() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("planner", List.of(tool("incidents.host-status"))));
        var llm = new AgentToolTestSupport.ScriptedLlm();
        NodeDefinition planner = agentNode("agent", Map.of(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true));

        executor(llm, new AtomicReference<>(AgentToolAccess.enabled(toolGate, 8)))
                .execute(context(graph("planner", planner), "run-1"), planner);

        assertThat(toolGate.listedFor).isEmpty();
        assertThat(llm.completions).hasSize(1);
    }

    @Test
    void toolGateOutageFailsTheNode() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of()) {
            @Override
            public List<org.olo.toolgate.client.ToolGateTool> listTools(String agentName) {
                throw new org.olo.toolgate.client.ToolGateException(
                        org.olo.toolgate.client.ToolGateException.Kind.UNAVAILABLE, "gateway down");
            }
        };
        NodeDefinition node = agentNode("agent", Map.of());

        NodeResult result = executor(new AgentToolTestSupport.ScriptedLlm(),
                new AtomicReference<>(AgentToolAccess.enabled(toolGate, 8)))
                .execute(context(graph("triage-agent", node), "run-1"), node);

        assertThat(result.status()).isEqualTo(NodeStatus.FAILED);
        assertThat(result.message()).contains("gateway down");
    }
}
