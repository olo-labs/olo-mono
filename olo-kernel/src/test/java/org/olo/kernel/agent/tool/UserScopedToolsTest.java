/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.input.model.Context;
import org.olo.input.model.IdentityBinding;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.agent.executor.impl.LocalLlmAgentExecutor;
import org.olo.kernel.agent.prompt.PromptRenderer;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;
import org.olo.toolgate.client.UserBinding;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.MODEL;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.agentNode;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.answer;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.callTool;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.graph;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.tool;

/** M3.3: user-scoped tools run as the run's verified user, or not at all. */
class UserScopedToolsTest {

    private static final AgentIdentityPolicy MAPPED = new MappedAgentIdentityPolicy(
            List.of(new MappedAgentIdentityPolicy.Mapping("*", "memory-agent", List.of("memory-agent"))),
            List.of("olo-platform-service"));

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

    private static KernelRuntimeContext context(WorkflowDefinition graph, String tenant, IdentityBinding identity) {
        Context runContext = new Context(tenant, null, List.of(), List.of(), "chat-1", "run-1", null, null, identity);
        return new KernelRuntimeContext("oloQueue2", new WorkflowInput("1.0", List.of(), runContext, null, null, null),
                graph, true, WorkflowRuntimeVariables.fromDefinition(graph));
    }

    private static NodeResult run(AgentToolTestSupport.FakeToolGate toolGate, AgentToolTestSupport.ScriptedLlm llm,
                                  KernelRuntimeContext context, NodeDefinition node) {
        var access = new AtomicReference<>(AgentToolAccess.userScoped(toolGate, 8, MAPPED));
        return AgentToolTestSupport.runTurn(
                new LocalLlmAgentExecutor(new DefaultLlmInvocationService(PROMPT, graph -> MODEL, llm, access::get)), context, node);
    }

    @Test
    void toolsRunAsTheRunsVerifiedUser() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("olo.memory.read"))));
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(callTool("olo_memory_read", "{}"), answer("done"));
        NodeDefinition node = agentNode("agent", Map.of());
        var identity = new IdentityBinding("t1", "user-42", "https://idp.example", "sid-1", 3);

        NodeResult result = run(toolGate, llm, context(graph("memory-agent", node), "t1", identity), node);

        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        // Every durable step authenticates afresh as the run's user (M3.3, M5.1); never as anyone else.
        assertThat(toolGate.users).isNotEmpty().containsOnly(new UserBinding("t1", "user-42", "https://idp.example", "sid-1", 3));
        assertThat(toolGate.calls).hasSize(1);
    }

    @Test
    void delegatedModeCallsAsTheAgentActingForTheRunsUser() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("olo.memory.read"))));
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(answer("done"));
        NodeDefinition node = agentNode("agent", Map.of());
        var identity = new IdentityBinding("t1", "user-42", "https://idp.example", "sid-1", 3);
        var access = new AtomicReference<>(AgentToolAccess.userScopedViaAgent(toolGate, 8, MAPPED));

        NodeResult result = AgentToolTestSupport.runTurn(
                new LocalLlmAgentExecutor(new DefaultLlmInvocationService(PROMPT, graph -> MODEL, llm, access::get)),
                context(graph("memory-agent", node), "t1", identity), node);

        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(toolGate.viaAgent).containsExactly(new UserBinding("t1", "user-42", "https://idp.example", "sid-1", 3));
        assertThat(toolGate.users).isEmpty();
        assertThat(toolGate.listedFor).containsExactly("memory-agent");
    }

    @Test
    void aRunWithoutAVerifiedUserNeverFallsBackToTheAgentToken() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("olo.memory.read"))));
        NodeDefinition node = agentNode("agent", Map.of());

        for (IdentityBinding identity : new IdentityBinding[]{
                null,
                new IdentityBinding("t1", "user-42", "https://idp.example", "", 3),
                new IdentityBinding("t1", "user-42", "https://idp.example", "sid-1", 0),
                new IdentityBinding("t2", "user-42", "https://idp.example", "sid-1", 3)}) {
            NodeResult result = run(toolGate, new AgentToolTestSupport.ScriptedLlm(),
                    context(graph("memory-agent", node), "t1", identity), node);
            assertThat(result.status()).isEqualTo(NodeStatus.FAILED);
            assertThat(result.message()).contains("no verified user");
        }
        assertThat(toolGate.users).isEmpty();
        assertThat(toolGate.listedFor).isEmpty();
        assertThat(toolGate.calls).isEmpty();
    }
}
