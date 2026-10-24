/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.toolgate.client.ToolGateCallResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.MODEL;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.answer;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.callTool;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.context;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.tool;

/** M5.3–M5.5: approvals resume the original call, unknown outcomes are never resent, old records are not sent blindly. */
class AgentTurnOutcomesTest {

    private final NodeDefinition node = agentNode("agent", Map.of());
    private final WorkflowDefinition graph = graph("triage-agent", node);
    private final AgentToolSettings settings = new AgentToolSettings("triage-agent", 8);
    private final AtomicLong now = new AtomicLong(1_800_000_000_000L);
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now.get()); }
    };

    private static ToolGateCallResult state(String state) {
        return new ToolGateCallResult(true, state, null, state, "inv-1");
    }

    private AgentToolTestSupport.FakeToolGate toolGate() {
        return new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.restart"))));
    }

    private AgentToolTestSupport.ScriptedLlm llm() {
        return new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_restart", "{\"host\":\"app-02\"}"), answer("restarted"));
    }

    @Test
    void aCallWaitsForApprovalAndThenResumesTheSameOperation() {
        var toolGate = toolGate();
        var calls = new AtomicInteger();
        toolGate.results = call -> calls.incrementAndGet() <= 2 ? state("PENDING_APPROVAL")
                : new ToolGateCallResult(false, "ok", null, null, "inv-1");
        var llm = llm();
        var loop = new AgentToolLoop(llm, clock);
        var context = context(graph, "run-1");

        assertThat(loop.step(context, node, MODEL, "restart", toolGate, settings).done()).isFalse(); // decide
        var waiting = loop.step(context, node, MODEL, "restart", toolGate, settings);
        assertThat(waiting.waiting()).isTrue();
        assertThat(context.getVariables().get(AgentToolLoop.RESUME_AFTER_VARIABLE)).isEqualTo(AgentToolLoop.APPROVAL_POLL_MS);
        now.addAndGet(AgentToolLoop.APPROVAL_POLL_MS);
        assertThat(loop.step(context, node, MODEL, "restart", toolGate, settings).waiting()).isTrue();
        now.addAndGet(AgentToolLoop.APPROVAL_POLL_MS);
        var resumed = loop.step(context, node, MODEL, "restart", toolGate, settings);
        assertThat(resumed.waiting()).isFalse();
        assertThat(context.getVariables().get(AgentToolLoop.RESUME_AFTER_VARIABLE)).isNull();
        var finished = loop.step(context, node, MODEL, "restart", toolGate, settings);

        assertThat(finished.result().response()).isEqualTo("restarted");
        assertThat(toolGate.calls).hasSize(3).extracting(AgentToolTestSupport.FakeToolGate.Call::requestId).containsOnly(toolGate.calls.get(0).requestId());
        assertThat(llm.chatMessages).hasSize(2); // the decision and the final answer; never re-asked while waiting
    }

    @Test
    void anApprovalThatIsNeverDecidedExpires() {
        var toolGate = toolGate();
        toolGate.results = call -> state("PENDING_APPROVAL");
        var loop = new AgentToolLoop(llm(), clock);
        var context = context(graph, "run-1");
        loop.step(context, node, MODEL, "restart", toolGate, settings);
        loop.step(context, node, MODEL, "restart", toolGate, settings);
        now.addAndGet(AgentToolLoop.APPROVAL_EXPIRY_MS + 1);
        var expired = loop.step(context, node, MODEL, "restart", toolGate, settings);
        assertThat(expired.done()).isTrue();
        assertThat(expired.result().outcome()).isEqualTo(AgentToolOutcome.APPROVAL_EXPIRED);
    }

    @Test
    void anUnknownOutcomeIsNeverResentAndStopsTheTurn() {
        var toolGate = toolGate();
        toolGate.results = call -> state("OUTCOME_UNKNOWN");
        var llm = llm();
        var loop = new AgentToolLoop(llm, clock);
        var context = context(graph, "run-1");
        loop.step(context, node, MODEL, "restart", toolGate, settings);
        var unknown = loop.step(context, node, MODEL, "restart", toolGate, settings);

        assertThat(unknown.done()).isTrue();
        assertThat(unknown.result().outcome()).isEqualTo(AgentToolOutcome.OUTCOME_UNKNOWN);
        assertThat(unknown.result().response()).contains("unknown").contains("inv-1").contains("not run again");
        assertThat(toolGate.calls).hasSize(1);
        assertThat(llm.chatMessages).hasSize(1);
    }

    @Test
    void anEndedInvocationIsAToolErrorTheModelSees() {
        for (String ended : List.of("FAILED", "CANCELLED", "EXPIRED", "PARTIAL")) {
            var toolGate = toolGate();
            toolGate.results = call -> state(ended);
            var llm = llm();
            var result = new AgentToolLoop(llm, clock).run(context(graph, "run-1"), node, MODEL, "restart", toolGate, settings);
            assertThat(result.outcome()).isEqualTo(AgentToolOutcome.COMPLETED);
            assertThat(result.invocations()).singleElement().extracting(ToolInvocationRecord::status).isEqualTo(ToolInvocationRecord.ERROR);
            assertThat(llm.chatMessages.get(1).get(llm.chatMessages.get(1).size() - 1).content()).startsWith("ERROR");
        }
    }

    @Test
    void aRecordedCallOlderThanTheDedupRetentionIsNeverSent() {
        var toolGate = toolGate();
        var loop = new AgentToolLoop(llm(), clock);
        var context = context(graph, "run-1");
        loop.step(context, node, MODEL, "restart", toolGate, settings);
        now.addAndGet(AgentToolLoop.DEDUP_RETENTION_MS + 1);
        var late = loop.step(context, node, MODEL, "restart", toolGate, settings);
        assertThat(late.result().outcome()).isEqualTo(AgentToolOutcome.OUTCOME_UNKNOWN);
        assertThat(toolGate.calls).isEmpty();
    }

    @Test
    void inMemoryRunsStopAtAnApprovalInsteadOfWaiting() {
        var toolGate = toolGate();
        toolGate.results = call -> state("PENDING_APPROVAL");
        var context = context(graph, "run-1");
        var result = new AgentToolLoop(llm(), clock).run(context, node, MODEL, "restart", toolGate, settings);
        assertThat(result.outcome()).isEqualTo(AgentToolOutcome.WAITING_APPROVAL);
        assertThat(context.getVariables().getString(AgentToolLoop.TURN_VARIABLE_PREFIX + "agent")).isNull();
    }
}
