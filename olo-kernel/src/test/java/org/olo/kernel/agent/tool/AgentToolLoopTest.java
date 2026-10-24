/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agent.client.LlmMessage;
import org.olo.kernel.agent.client.LlmToolSpec;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.toolgate.client.ToolGateCallResult;
import org.olo.toolgate.client.ToolGateException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.MODEL;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.answer;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.callTool;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.context;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.tool;

class AgentToolLoopTest {

    private final NodeDefinition node = agentNode("agent", Map.of());
    private final WorkflowDefinition graph = graph("triage-agent", node);

    private AgentToolSettings settings(String agentName, int rounds) {
        return new AgentToolSettings(agentName, rounds);
    }

    @Test
    void emptyCatalogRunsThePlainPrompt() {
        var llm = new AgentToolTestSupport.ScriptedLlm();
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of());

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "hello", toolGate, settings("triage-agent", 8));

        assertThat(result.response()).isEqualTo("plain answer");
        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.COMPLETED);
        assertThat(llm.completions).containsExactly("hello");
        assertThat(llm.chatMessages).isEmpty();
        assertThat(toolGate.listedFor).containsExactly("triage-agent");
    }

    @Test
    void callsAToolThroughToolGateAndHandsTheResultBack() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(
                callTool("incidents_host-status", "{\"host\":\"app-02\"}"),
                answer("app-02 runs out of memory"));
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of(
                "triage-agent", List.of(tool("incidents.host-status"))));
        toolGate.results = call -> new ToolGateCallResult(false, "text",
                AgentToolTestSupport.json("{\"host\":\"app-02\",\"memory\":\"99%\"}"), null, "inv-1");

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "why did app-02 crash?", toolGate, settings("triage-agent", 8));

        assertThat(result.response()).isEqualTo("app-02 runs out of memory");
        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.COMPLETED);
        assertThat(result.catalog()).containsExactly("incidents.host-status");

        // The model sees a function-call-safe name; ToolGate gets the real one, under the agent's identity.
        assertThat(llm.chatTools.get(0)).extracting(LlmToolSpec::name).containsExactly("incidents_host-status");
        assertThat(toolGate.calls).singleElement().satisfies(call -> {
            assertThat(call.agentName()).isEqualTo("triage-agent");
            assertThat(call.tool()).isEqualTo("incidents.host-status");
            assertThat(call.arguments().path("host").asText()).isEqualTo("app-02");
            assertThat(call.requestId()).startsWith("olo-op-").hasSize(55);
        });

        List<LlmMessage> second = llm.chatMessages.get(1);
        assertThat(second).extracting(LlmMessage::role).containsExactly("user", "assistant", "tool");
        assertThat(second.get(2).content()).isEqualTo("{\"host\":\"app-02\",\"memory\":\"99%\"}");
        assertThat(second.get(2).toolName()).isEqualTo("incidents_host-status");

        assertThat(result.invocations()).singleElement().satisfies(record -> {
            assertThat(record.status()).isEqualTo(ToolInvocationRecord.OK);
            assertThat(record.invocationId()).isEqualTo("inv-1");
            assertThat(record.round()).isEqualTo(1);
        });
    }

    @Test
    void toolErrorsAreShownToTheModelNotThrown() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(
                callTool("incidents_remediate", "{}"), answer("I may not remediate"));
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.remediate"))));
        toolGate.results = call -> ToolGateCallResult.failure("Tool 'incidents.remediate' is not available to agent 'triage-agent'.");

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "fix it", toolGate, settings("triage-agent", 8));

        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.COMPLETED);
        assertThat(llm.chatMessages.get(1).get(2).content()).startsWith("ERROR: ").contains("not available");
        assertThat(result.invocations()).extracting(ToolInvocationRecord::status).containsExactly(ToolInvocationRecord.ERROR);
    }

    @Test
    void approvalRequiredStopsTheTurnWithoutCallingTheModelAgain() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_remediate", "{\"action\":\"ROLLBACK_DEPLOY\"}"));
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("remediation-agent", List.of(tool("incidents.remediate"))));
        toolGate.results = call -> new ToolGateCallResult(true, "Approval required",
                AgentToolTestSupport.json("{\"invocationId\":\"inv-9\",\"state\":\"PENDING_APPROVAL\"}"),
                "PENDING_APPROVAL", "inv-9");

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "roll back", toolGate, settings("remediation-agent", 8));

        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.WAITING_APPROVAL);
        assertThat(result.response()).contains("incidents.remediate").contains("inv-9").contains("Nothing was run");
        assertThat(llm.chatMessages).hasSize(1);
        assertThat(result.invocations()).extracting(ToolInvocationRecord::status)
                .containsExactly(ToolInvocationRecord.PENDING_APPROVAL);
    }

    @Test
    void unknownToolAndBadArgumentsNeverReachToolGate() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(
                new org.olo.kernel.agent.client.LlmChatResponse("", List.of(
                        new org.olo.kernel.agent.client.LlmToolCall("shell_exec", AgentToolTestSupport.json("{}"), "{}"),
                        new org.olo.kernel.agent.client.LlmToolCall("incidents_host-status", null, "not json"))),
                answer("done"));
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.host-status"))));

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "go", toolGate, settings("triage-agent", 8));

        assertThat(toolGate.calls).isEmpty();
        List<LlmMessage> second = llm.chatMessages.get(1);
        assertThat(second.get(2).content()).contains("no tool named 'shell_exec'");
        assertThat(second.get(3).content()).contains("must be a JSON object");
        assertThat(result.invocations()).extracting(ToolInvocationRecord::status)
                .containsExactly(ToolInvocationRecord.INVALID_CALL, ToolInvocationRecord.INVALID_CALL);
    }

    @Test
    void roundLimitAsksForAFinalAnswerWithoutTools() {
        var llm = new AgentToolTestSupport.ScriptedLlm()
                .repeatForever(callTool("incidents_host-status", "{\"host\":\"app-02\"}"));
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.host-status"))));

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "go", toolGate, settings("triage-agent", 2));

        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.ROUND_LIMIT);
        assertThat(result.response()).isEqualTo("final after limit");
        assertThat(toolGate.calls).hasSize(2);
        assertThat(llm.chatTools).hasSize(3);
        assertThat(llm.chatTools.get(2)).isEmpty();
        List<LlmMessage> last = llm.chatMessages.get(2);
        assertThat(last.get(last.size() - 1).content()).isEqualTo(AgentToolLoop.ROUND_LIMIT_PROMPT);
    }

    @Test
    void zeroRoundsTurnsToolsOffForThatAgent() {
        var llm = new AgentToolTestSupport.ScriptedLlm();
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.host-status"))));

        AgentToolLoopResult result = new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "hi", toolGate, settings("triage-agent", 0));

        assertThat(result.response()).isEqualTo("plain answer");
        assertThat(toolGate.calls).isEmpty();
    }

    @Test
    void requestIdsRepeatOnActivityRetryAndDifferPerPass() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.host-status"))));
        Map<String, Object> snapshot = new HashMap<>();

        // A Temporal retry re-runs the step from the same snapshot: same ids, so ToolGate returns the earlier result.
        runOnce(context(graph, "run-1", snapshot), toolGate);
        runOnce(context(graph, "run-1", snapshot), toolGate);
        assertThat(toolGate.calls.get(0).requestId()).isEqualTo(toolGate.calls.get(1).requestId());

        // A later pass of the same node in the same run is a new invocation.
        KernelRuntimeContext shared = context(graph, "run-1", snapshot);
        runOnce(shared, toolGate);
        runOnce(shared, toolGate);
        assertThat(toolGate.calls.get(3).requestId()).isNotEqualTo(toolGate.calls.get(2).requestId());

        // Another run never collides.
        runOnce(context(graph, "run-2", snapshot), toolGate);
        assertThat(toolGate.calls.get(4).requestId()).isNotEqualTo(toolGate.calls.get(0).requestId());
    }

    @Test
    void toolGateOutagesFailTheTurn() {
        var llm = new AgentToolTestSupport.ScriptedLlm();
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of()) {
            @Override
            public List<org.olo.toolgate.client.ToolGateTool> listTools(String agentName) {
                throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "gateway down");
            }
        };

        assertThatThrownBy(() -> new AgentToolLoop(llm)
                .run(context(graph, "run-1"), node, MODEL, "hi", toolGate, settings("triage-agent", 8)))
                .isInstanceOf(ToolGateException.class)
                .hasMessageContaining("gateway down");
        assertThat(llm.completions).isEmpty();
    }

    @Test
    void longToolResultsAreTruncatedForTheModel() {
        String big = "x".repeat(AgentToolLoop.MAX_TOOL_RESULT_CHARS + 50);
        String shown = AgentToolLoop.forModel(new ToolGateCallResult(false, big, null, null, null));

        assertThat(shown).hasSize(AgentToolLoop.MAX_TOOL_RESULT_CHARS + "... [truncated]".length()).endsWith("[truncated]");
    }

    private void runOnce(KernelRuntimeContext context, AgentToolTestSupport.FakeToolGate toolGate) {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(
                callTool("incidents_host-status", "{\"host\":\"app-02\"}"), answer("done"));
        new AgentToolLoop(llm).run(context, node, MODEL, "go", toolGate, settings("triage-agent", 8));
    }
}
