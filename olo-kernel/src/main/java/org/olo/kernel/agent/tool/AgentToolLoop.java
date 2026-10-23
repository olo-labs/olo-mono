/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.agent.client.LlmChatResponse;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.client.LlmMessage;
import org.olo.kernel.agent.client.LlmToolCall;
import org.olo.kernel.agent.client.LlmToolSpec;
import org.olo.kernel.agent.model.ResolvedModelCall;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.toolgate.client.ToolGateCallResult;
import org.olo.toolgate.client.ToolGateClient;
import org.olo.toolgate.client.ToolGateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs one agent turn with the tools ToolGate allows for the agent's identity.
 *
 * <ol>
 *   <li>{@code tools/list} under {@code agentName}. An empty catalog runs the plain prompt, as without ToolGate.</li>
 *   <li>Chat with native tool calling. Each requested call goes to ToolGate {@code tools/call} under the same
 *       identity, and its result is handed back to the model.</li>
 *   <li>Stops at the model's final answer, at the first call that needs approval, or after
 *       {@code maxToolRounds} rounds (then the model is asked to answer without tools).</li>
 * </ol>
 *
 * <p>This runs inside the traversal-step activity, so it never executes in Temporal workflow code.
 * ToolGate failures are thrown and fail the step: the agent never continues without governance.
 */
public final class AgentToolLoop {

    /** Run variable counting how often an AGENT node has run, so repeated passes get distinct request ids. */
    public static final String PASS_VARIABLE_PREFIX = "__olo.toolgate.pass.";

    static final int MAX_TOOL_RESULT_CHARS = 8_000;
    static final String ROUND_LIMIT_PROMPT =
            "The tool budget for this turn is used up. Answer the user now with what you have."
                    + " Say plainly what you could not check.";

    private static final Logger log = LoggerFactory.getLogger(AgentToolLoop.class);

    private final LlmClient llmClient;

    public AgentToolLoop(LlmClient llmClient) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    public AgentToolLoopResult run(
            KernelRuntimeContext context,
            NodeDefinition node,
            ResolvedModelCall modelCall,
            String prompt,
            ToolGateClient toolGate,
            AgentToolSettings settings) {
        Objects.requireNonNull(toolGate, "toolGate");
        Objects.requireNonNull(settings, "settings");
        String agentName = settings.agentName();

        List<ToolGateTool> catalog = toolGate.listTools(agentName);
        log.info("Agent node {} as '{}': ToolGate catalog {}", node.getId(), agentName,
                catalog.stream().map(ToolGateTool::name).toList());
        if (catalog.isEmpty() || settings.maxToolRounds() == 0) {
            return new AgentToolLoopResult(llmClient.complete(modelCall, prompt), AgentToolOutcome.COMPLETED,
                    agentName, names(catalog), List.of());
        }

        ToolNameMapper names = new ToolNameMapper(catalog.stream().map(ToolGateTool::name).toList());
        List<LlmToolSpec> specs = catalog.stream()
                .map(tool -> new LlmToolSpec(names.modelName(tool.name()), tool.description(), tool.inputSchema()))
                .toList();
        int pass = nextPass(context, node);
        String runId = runId(context);
        String scope = (context.getGraph() != null ? context.getGraph().getId() : "") + "/" + node.getId();

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.user(prompt));
        List<ToolInvocationRecord> invocations = new ArrayList<>();

        for (int round = 1; round <= settings.maxToolRounds(); round++) {
            LlmChatResponse reply = llmClient.chat(modelCall, messages, specs);
            if (!reply.hasToolCalls()) {
                return new AgentToolLoopResult(reply.content(), AgentToolOutcome.COMPLETED,
                        agentName, names(catalog), invocations);
            }
            messages.add(LlmMessage.assistant(reply.content(), reply.toolCalls()));

            int index = 0;
            for (LlmToolCall call : reply.toolCalls()) {
                index++;
                Optional<String> toolGateName = names.toolGateName(call.name());
                if (toolGateName.isEmpty()) {
                    invocations.add(new ToolInvocationRecord(call.name(), null, round, ToolInvocationRecord.INVALID_CALL, null));
                    messages.add(LlmMessage.toolResult(call.name(),
                            "ERROR: there is no tool named '" + call.name() + "'. Use only the tools you were given."));
                    continue;
                }
                if (call.arguments() == null) {
                    invocations.add(new ToolInvocationRecord(toolGateName.get(), null, round, ToolInvocationRecord.INVALID_CALL, null));
                    messages.add(LlmMessage.toolResult(call.name(),
                            "ERROR: the arguments must be a JSON object, got: " + truncate(call.rawArguments(), 300)));
                    continue;
                }

                String requestId = ToolCallRequestIds.requestId(
                        runId, scope, pass, round, index, toolGateName.get(), call.arguments());
                ToolGateCallResult result = toolGate.callTool(agentName, requestId, toolGateName.get(), call.arguments());

                if (result.pendingApproval()) {
                    invocations.add(new ToolInvocationRecord(toolGateName.get(), requestId, round,
                            ToolInvocationRecord.PENDING_APPROVAL, result.invocationId()));
                    log.info("Agent node {} as '{}': {} waits for approval (invocation {})",
                            node.getId(), agentName, toolGateName.get(), result.invocationId());
                    return new AgentToolLoopResult(waitingMessage(toolGateName.get(), result.invocationId()),
                            AgentToolOutcome.WAITING_APPROVAL, agentName, names(catalog), invocations);
                }
                invocations.add(new ToolInvocationRecord(toolGateName.get(), requestId, round,
                        result.error() ? ToolInvocationRecord.ERROR : ToolInvocationRecord.OK, result.invocationId()));
                messages.add(LlmMessage.toolResult(call.name(), forModel(result)));
            }
        }

        messages.add(LlmMessage.user(ROUND_LIMIT_PROMPT));
        LlmChatResponse finalReply = llmClient.chat(modelCall, messages, List.of());
        return new AgentToolLoopResult(finalReply.content(), AgentToolOutcome.ROUND_LIMIT,
                agentName, names(catalog), invocations);
    }

    static String forModel(ToolGateCallResult result) {
        String body;
        JsonNode structured = result.structuredContent();
        if (structured != null && !structured.isNull() && !structured.isMissingNode()) {
            body = structured.toString();
        } else {
            body = result.text();
        }
        body = truncate(body, MAX_TOOL_RESULT_CHARS);
        return result.error() ? "ERROR: " + body : body;
    }

    static String waitingMessage(String tool, String invocationId) {
        return "Waiting for approval in ToolGate before running '" + tool + "'"
                + (invocationId != null ? " (invocation " + invocationId + ")" : "")
                + ". Nothing was run for it. Run this request again after it is approved.";
    }

    private static int nextPass(KernelRuntimeContext context, NodeDefinition node) {
        String key = PASS_VARIABLE_PREFIX + node.getId();
        Object current = context.getVariables().get(key);
        int pass = current instanceof Number number ? number.intValue() : 0;
        context.getVariables().set(key, pass + 1);
        return pass;
    }

    private static String runId(KernelRuntimeContext context) {
        if (context.getInput() == null || context.getInput().getContext() == null) {
            return null;
        }
        return context.getInput().getContext().getRunId();
    }

    private static List<String> names(List<ToolGateTool> catalog) {
        return catalog.stream().map(ToolGateTool::name).toList();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "... [truncated]";
    }
}
