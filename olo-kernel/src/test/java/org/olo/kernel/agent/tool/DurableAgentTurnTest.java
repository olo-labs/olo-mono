/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.KernelRuntimeContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.MODEL;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.answer;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.callTool;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.context;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.tool;

/**
 * M5.1 (M1-R1): the model's decision is recorded before anything is dispatched, and recovery resumes recorded
 * operations instead of asking the model again. A "retry" here re-runs a step from the run variables as they were
 * persisted before it, which is what Temporal does when an activity fails.
 */
class DurableAgentTurnTest {

    private final NodeDefinition node = agentNode("agent", Map.of());
    private final WorkflowDefinition graph = graph("triage-agent", node);
    private final AgentToolSettings settings = new AgentToolSettings("triage-agent", 8);

    private AgentToolTestSupport.FakeToolGate toolGate() {
        return new AgentToolTestSupport.FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.host-status"))));
    }

    private KernelRuntimeContext persisted(KernelRuntimeContext context) {
        return context(graph, "run-1", context.getVariables().toMap());
    }

    @Test
    void theDecisionIsRecordedBeforeAnyDispatch() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_host-status", "{\"host\":\"app-02\"}"));
        var toolGate = toolGate();
        var context = context(graph, "run-1");

        AgentToolLoop.Step decided = new AgentToolLoop(llm).step(context, node, MODEL, "why?", toolGate, settings);

        assertThat(decided.done()).isFalse();
        assertThat(toolGate.calls).isEmpty();
        assertThat(context.getVariables().getString(AgentToolLoop.TURN_VARIABLE_PREFIX + "agent"))
                .contains("olo-op-").contains("incidents.host-status").contains("app-02");
    }

    @Test
    void aRetriedDispatchResendsTheRecordedOperationsAndNeverAsksTheModelAgain() {
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(
                callTool("incidents_host-status", "{\"host\":\"app-02\"}"), answer("app-02 is out of memory"));
        var toolGate = toolGate();
        var loop = new AgentToolLoop(llm);
        var context = context(graph, "run-1");
        loop.step(context, node, MODEL, "why?", toolGate, settings);
        KernelRuntimeContext beforeDispatch = persisted(context);

        // The dispatch activity sends the call, then fails before its result is persisted.
        loop.step(persisted(beforeDispatch), node, MODEL, "why?", toolGate, settings);
        // Temporal retries it from the persisted decision.
        var retried = persisted(beforeDispatch);
        loop.step(retried, node, MODEL, "why?", toolGate, settings);

        assertThat(llm.chatMessages).hasSize(1);
        assertThat(toolGate.calls).hasSize(2);
        assertThat(toolGate.calls.get(1).requestId()).isEqualTo(toolGate.calls.get(0).requestId());
        assertThat(toolGate.calls.get(1).arguments()).isEqualTo(toolGate.calls.get(0).arguments());

        AgentToolLoop.Step finished = loop.step(retried, node, MODEL, "why?", toolGate, settings);
        assertThat(finished.done()).isTrue();
        assertThat(finished.result().response()).isEqualTo("app-02 is out of memory");
        assertThat(retried.getVariables().getString(AgentToolLoop.TURN_VARIABLE_PREFIX + "agent")).isNull();
    }

    @Test
    void aRetriedDecisionKeepsTheOperationIdsEvenIfTheModelChoosesDifferentArguments() {
        var first = new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_host-status", "{\"host\":\"app-02\"}"));
        var second = new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_host-status", "{\"host\":\"app-03\"}"));
        var start = context(graph, "run-1");
        var a = persisted(start);
        var b = persisted(start);
        new AgentToolLoop(first).step(a, node, MODEL, "why?", toolGate(), settings);
        new AgentToolLoop(second).step(b, node, MODEL, "why?", toolGate(), settings);

        // Nothing was dispatched for the lost decision; if it had been, ToolGate refuses the changed arguments under the
        // same logical-operation id as a conflict (M2.4) instead of running a second effect.
        String idA = operationId(a);
        assertThat(idA).isEqualTo(operationId(b));
    }

    @Test
    void separatePassesOfTheSameNodeGetDistinctOperationIds() {
        var llm = new AgentToolTestSupport.ScriptedLlm()
                .reply(callTool("incidents_host-status", "{\"host\":\"app-02\"}"), answer("one"))
                .reply(callTool("incidents_host-status", "{\"host\":\"app-02\"}"), answer("two"));
        var toolGate = toolGate();
        var context = context(graph, "run-1");
        new AgentToolLoop(llm).run(context, node, MODEL, "why?", toolGate, settings);
        new AgentToolLoop(llm).run(context, node, MODEL, "why?", toolGate, settings);

        assertThat(toolGate.calls).hasSize(2);
        assertThat(toolGate.calls.get(0).requestId()).isNotEqualTo(toolGate.calls.get(1).requestId());
    }

    private static String operationId(KernelRuntimeContext context) {
        String turn = context.getVariables().getString(AgentToolLoop.TURN_VARIABLE_PREFIX + "agent");
        int at = turn.indexOf("olo-op-");
        return turn.substring(at, at + 55);
    }
}
