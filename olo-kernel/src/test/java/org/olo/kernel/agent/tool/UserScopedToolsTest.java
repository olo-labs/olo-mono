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
    void anExpiredSessionOrRevokedGrantDuringApprovalEndsTheTurnWithoutAnotherDispatch() {
        for (var reason : java.util.List.of(org.olo.toolgate.client.ToolGateException.Kind.UNAUTHORIZED,
                org.olo.toolgate.client.ToolGateException.Kind.FORBIDDEN)) {
            var active=new java.util.concurrent.atomic.AtomicBoolean(true);
            var gateway=new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent",List.of(tool("incidents.restart")))) {
                @Override public org.olo.toolgate.client.ToolGateClient forUserViaAgent(UserBinding binding) {
                    if(!active.get()) throw new org.olo.toolgate.client.ToolGateException(reason,"current user authority refused");
                    return super.forUserViaAgent(binding);
                }
            };
            gateway.results=call->new org.olo.toolgate.client.ToolGateCallResult(true,"approval",null,"PENDING_APPROVAL","inv-original");
            var llm=new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_restart","{}"));
            var node=agentNode("agent",Map.of());
            var identity=new IdentityBinding("t1","user-42","https://idp.example","sid-1",3);
            var context=context(graph("memory-agent",node),"t1",identity);
            var access=AgentToolAccess.userScopedViaAgent(gateway,8,MAPPED);
            var executor=new LocalLlmAgentExecutor(new DefaultLlmInvocationService(PROMPT,graph->MODEL,llm,()->access));
            assertThat(executor.execute(context,node).status()).isEqualTo(NodeStatus.CONTINUE);
            assertThat(executor.execute(context,node).status()).isEqualTo(NodeStatus.CONTINUE);
            active.set(false);
            assertThat(executor.execute(context,node).status()).isEqualTo(NodeStatus.FAILED);
            assertThat(gateway.calls).hasSize(1);assertThat(llm.chatMessages).hasSize(1);assertThat(gateway.users).isEmpty();
        }
    }

    @Test
    void toolsRunAsTheRunsVerifiedUser() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("incidents.read"))));
        var llm = new AgentToolTestSupport.ScriptedLlm().reply(callTool("incidents_read", "{}"), answer("done"));
        NodeDefinition node = agentNode("agent", Map.of());
        var identity = new IdentityBinding("t1", "user-42", "https://idp.example", "sid-1", 3);

        NodeResult result = run(toolGate, llm, context(graph("memory-agent", node), "t1", identity), node);

        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        // Every durable step authenticates afresh as the run's user (M3.3, M5.1); never as anyone else.
        assertThat(toolGate.viaAgent).isNotEmpty().containsOnly(new UserBinding("t1", "user-42", "https://idp.example", "sid-1", 3));
        assertThat(toolGate.users).isEmpty();
        assertThat(toolGate.calls).hasSize(1);
    }

    @Test
    void delegatedModeCallsAsTheAgentActingForTheRunsUser() {
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("incidents.read"))));
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
        var toolGate = new AgentToolTestSupport.FakeToolGate(Map.of("memory-agent", List.of(tool("incidents.read"))));
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
