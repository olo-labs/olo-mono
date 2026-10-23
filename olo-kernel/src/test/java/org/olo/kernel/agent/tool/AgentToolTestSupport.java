/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.Context;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.agent.client.LlmChatResponse;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.client.LlmMessage;
import org.olo.kernel.agent.client.LlmToolCall;
import org.olo.kernel.agent.client.LlmToolSpec;
import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.toolgate.client.ToolGateCallResult;
import org.olo.toolgate.client.ToolGateClient;
import org.olo.toolgate.client.ToolGateTool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Scripted LLM, fake ToolGate and context helpers for agent tool-loop tests.
 */
final class AgentToolTestSupport {

    static final ObjectMapper MAPPER = new ObjectMapper();
    static final ResolvedModelCall MODEL = new ResolvedModelCall("p", "ollama", "llama3.2:latest", "http://ollama", 0.0);

    private AgentToolTestSupport() {
    }

    static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static LlmChatResponse callTool(String name, String argumentsJson) {
        return new LlmChatResponse("", List.of(new LlmToolCall(name, json(argumentsJson), argumentsJson)));
    }

    static LlmChatResponse answer(String text) {
        return new LlmChatResponse(text, List.of());
    }

    static NodeDefinition agentNode(String id, Map<String, Object> configuration) {
        return NodeDefinition.builder().id(id).type(NodeType.AGENT.name()).configuration(configuration).build();
    }

    static WorkflowDefinition graph(String workflowId, NodeDefinition... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.create(workflowId).id(workflowId).queue("oloQueue2");
        for (NodeDefinition node : nodes) {
            builder.addNode(node);
        }
        return builder.build();
    }

    static KernelRuntimeContext context(WorkflowDefinition graph, String runId) {
        Context runContext = new Context(null, null, List.of(), List.of(), "session-1", runId, null, null);
        return new KernelRuntimeContext(
                "oloQueue2",
                new WorkflowInput("1.0", List.of(), runContext, null, null, null),
                graph,
                true,
                WorkflowRuntimeVariables.fromDefinition(graph));
    }

    static KernelRuntimeContext context(WorkflowDefinition graph, String runId, Map<String, Object> variables) {
        Context runContext = new Context(null, null, List.of(), List.of(), "session-1", runId, null, null);
        return new KernelRuntimeContext(
                "oloQueue2",
                new WorkflowInput("1.0", List.of(), runContext, null, null, null),
                graph,
                true,
                WorkflowRuntimeVariables.fromMap(variables));
    }

    /** Replays scripted chat replies and records what the loop sent. */
    static final class ScriptedLlm implements LlmClient {

        final Deque<LlmChatResponse> replies = new ArrayDeque<>();
        final List<List<LlmMessage>> chatMessages = new ArrayList<>();
        final List<List<LlmToolSpec>> chatTools = new ArrayList<>();
        final List<String> completions = new ArrayList<>();
        LlmChatResponse repeat;

        ScriptedLlm reply(LlmChatResponse... responses) {
            replies.addAll(List.of(responses));
            return this;
        }

        ScriptedLlm repeatForever(LlmChatResponse response) {
            this.repeat = response;
            return this;
        }

        @Override
        public String complete(ResolvedModelCall modelCall, String prompt) {
            completions.add(prompt);
            return "plain answer";
        }

        @Override
        public LlmChatResponse chat(ResolvedModelCall modelCall, List<LlmMessage> messages, List<LlmToolSpec> tools) {
            chatMessages.add(List.copyOf(messages));
            chatTools.add(List.copyOf(tools));
            if (!replies.isEmpty()) {
                return replies.poll();
            }
            if (repeat != null && !tools.isEmpty()) {
                return repeat;
            }
            return answer("final after limit");
        }
    }

    /** Per-agent catalogs and scripted tool results. */
    static class FakeToolGate implements ToolGateClient {

        record Call(String agentName, String requestId, String tool, JsonNode arguments) {
        }

        final Map<String, List<ToolGateTool>> catalogs;
        final List<String> listedFor = new ArrayList<>();
        final List<Call> calls = new ArrayList<>();
        final List<org.olo.toolgate.client.UserBinding> users = new ArrayList<>();
        Function<Call, ToolGateCallResult> results = call ->
                new ToolGateCallResult(false, "ok", json("{\"tool\":\"" + call.tool() + "\"}"), null, null);

        FakeToolGate(Map<String, List<ToolGateTool>> catalogs) {
            this.catalogs = catalogs;
        }

        @Override
        public List<ToolGateTool> listTools(String agentName) {
            listedFor.add(agentName);
            return catalogs.getOrDefault(agentName, List.of());
        }

        @Override
        public ToolGateCallResult callTool(String agentName, String requestId, String toolName, JsonNode arguments) {
            Call call = new Call(agentName, requestId, toolName, arguments);
            calls.add(call);
            return results.apply(call);
        }

        @Override
        public void probe() {
        }

        /** Records the user and keeps recording calls on the same fake. */
        @Override
        public ToolGateClient forUser(org.olo.toolgate.client.UserBinding binding) {
            users.add(binding);
            return this;
        }
    }

    static ToolGateTool tool(String name) {
        return new ToolGateTool(name, "does " + name,
                json("{\"type\":\"object\",\"properties\":{\"host\":{\"type\":\"string\"}}}"));
    }
}
