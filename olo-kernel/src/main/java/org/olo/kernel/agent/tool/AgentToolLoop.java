/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p><b>Durable turn (M5.1, M1-R1).</b> {@link #step} does one phase and keeps the turn in a run variable:
 * <ul>
 *   <li><i>Decide:</i> asks the model and <em>records</em> the calls it selected, each with a stable logical-operation id
 *       that depends on the run, node, pass, round and position, never on the arguments. Nothing is dispatched.</li>
 *   <li><i>Dispatch:</i> sends exactly the recorded calls. A retried dispatch re-sends the same ids, so ToolGate
 *       returns the same invocations; the model is never asked again for an operation already recorded.</li>
 * </ul>
 * The traversal persists the run variables between steps (in Temporal, between activities), so a recorded decision is in
 * workflow history before it is acted on. {@link #run} drives the phases in memory, for callers without that boundary.
 *
 * <p>This runs inside the traversal-step activity, so it never executes in Temporal workflow code.
 * ToolGate failures are thrown and fail the step: the agent never continues without governance.
 */
public final class AgentToolLoop {

    /** Run variable counting how often an AGENT node has run, so repeated passes get distinct operation ids. */
    public static final String PASS_VARIABLE_PREFIX = "__olo.toolgate.pass.";
    /** Run variable holding an unfinished turn of an AGENT node: model messages, recorded and dispatched calls. */
    public static final String TURN_VARIABLE_PREFIX = "__olo.toolgate.turn.";

    static final int MAX_TOOL_RESULT_CHARS = 8_000;
    static final String ROUND_LIMIT_PROMPT =
            "The tool budget for this turn is used up. Answer the user now with what you have."
                    + " Say plainly what you could not check.";

    private static final Logger log = LoggerFactory.getLogger(AgentToolLoop.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Run variable: wait this many milliseconds before the next step (an approval wait, M5.5); the workflow sleeps. */
    public static final String RESUME_AFTER_VARIABLE = "__olo.kernel.resumeAfterMs";
    /** How long ToolGate keeps dedup records (M2.4); an older recorded call is never sent (M5.3). */
    public static final long DEDUP_RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    /** How long a call may wait for approval (M2.4). */
    public static final long APPROVAL_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;
    /** How often a waiting call is checked again. */
    public static final long APPROVAL_POLL_MS = 30_000;

    private final LlmClient llmClient;
    private final java.time.Clock clock;

    public AgentToolLoop(LlmClient llmClient) {
        this(llmClient, java.time.Clock.systemUTC());
    }

    AgentToolLoop(LlmClient llmClient, java.time.Clock clock) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The outcome of one {@link #step}: the finished turn, or recorded progress with more to do. */
    public record Step(AgentToolLoopResult result, String progress, long resumeAfterMs) {
        public Step(AgentToolLoopResult result, String progress) {
            this(result, progress, 0);
        }

        public boolean done() {
            return result != null;
        }

        /** Whether the turn is waiting for an approval before its next step. */
        public boolean waiting() {
            return resumeAfterMs > 0;
        }
    }

    /**
     * A call the model selected, recorded before it is dispatched. {@code status} is null until the call has a result;
     * a dispatch resumed after a wait skips calls that already have one.
     */
    record RecordedCall(String operationId, String modelName, String tool, JsonNode arguments, long recordedAtUnixMs,
                        String status, String invocationId) {
        RecordedCall done(String status, String invocationId) {
            return new RecordedCall(operationId, modelName, tool, arguments, recordedAtUnixMs, status, invocationId);
        }
    }

    /** An unfinished turn, kept in {@link #TURN_VARIABLE_PREFIX}{@code <nodeId>} between steps. */
    record Turn(int pass, int round, List<ToolGateTool> catalog, List<LlmMessage> messages,
                List<RecordedCall> pending, List<ToolInvocationRecord> invocations, Long waitingSinceUnixMs) {
        Turn {
            catalog = catalog == null ? List.of() : List.copyOf(catalog);
            messages = messages == null ? List.of() : List.copyOf(messages);
            pending = pending == null ? List.of() : List.copyOf(pending);
            invocations = invocations == null ? List.of() : List.copyOf(invocations);
        }
    }

    /** The whole turn in one call: every phase, in memory. */
    public AgentToolLoopResult run(
            KernelRuntimeContext context,
            NodeDefinition node,
            ResolvedModelCall modelCall,
            String prompt,
            ToolGateClient toolGate,
            AgentToolSettings settings) {
        while (true) {
            Step step = step(context, node, modelCall, prompt, toolGate, settings);
            if (step.done()) {
                return step.result();
            }
            if (step.waiting()) {
                // In memory there is no durable wait: stop, as before, and let the caller run the request again.
                Turn turn = load(context, node);
                List<ToolInvocationRecord> invocations = new ArrayList<>(turn.invocations());
                turn.pending().stream().filter(c -> c.status() == null).findFirst().ifPresent(c -> invocations.add(
                        new ToolInvocationRecord(c.tool(), c.operationId(), turn.round(), ToolInvocationRecord.PENDING_APPROVAL, c.invocationId())));
                return finish(context, node, new AgentToolLoopResult(waitingMessage(turn), AgentToolOutcome.WAITING_APPROVAL,
                        settings.agentName(), names(turn.catalog()), invocations)).result();
            }
        }
    }

    /** One durable phase of the turn: decide (and record) or dispatch (what was recorded). */
    public Step step(
            KernelRuntimeContext context,
            NodeDefinition node,
            ResolvedModelCall modelCall,
            String prompt,
            ToolGateClient toolGate,
            AgentToolSettings settings) {
        Objects.requireNonNull(toolGate, "toolGate");
        Objects.requireNonNull(settings, "settings");
        String agentName = settings.agentName();
        context.getVariables().set(RESUME_AFTER_VARIABLE, null);

        Turn turn = load(context, node);
        if (turn == null) {
            List<ToolGateTool> catalog = toolGate.listTools(agentName);
            log.info("Agent node {} as '{}': ToolGate catalog {}", node.getId(), agentName,
                    catalog.stream().map(ToolGateTool::name).toList());
            int pass = nextPass(context, node);
            if (catalog.isEmpty() || settings.maxToolRounds() == 0) {
                return finish(context, node, new AgentToolLoopResult(llmClient.complete(modelCall, prompt),
                        AgentToolOutcome.COMPLETED, agentName, names(catalog), List.of()));
            }
            turn = new Turn(pass, 1, catalog, List.of(LlmMessage.user(prompt)), List.of(), List.of(), null);
        }
        return turn.pending().isEmpty()
                ? decide(context, node, modelCall, toolGate, settings, turn)
                : dispatch(context, node, toolGate, agentName, turn);
    }

    private Step decide(KernelRuntimeContext context, NodeDefinition node, ResolvedModelCall modelCall,
                        ToolGateClient toolGate, AgentToolSettings settings, Turn turn) {
        String agentName = settings.agentName();
        List<String> catalogNames = names(turn.catalog());
        List<LlmMessage> messages = new ArrayList<>(turn.messages());
        if (turn.round() > settings.maxToolRounds()) {
            messages.add(LlmMessage.user(ROUND_LIMIT_PROMPT));
            LlmChatResponse finalReply = llmClient.chat(modelCall, messages, List.of());
            return finish(context, node, new AgentToolLoopResult(finalReply.content(), AgentToolOutcome.ROUND_LIMIT,
                    agentName, catalogNames, turn.invocations()));
        }

        ToolNameMapper names = new ToolNameMapper(catalogNames);
        List<LlmToolSpec> specs = turn.catalog().stream()
                .map(tool -> new LlmToolSpec(names.modelName(tool.name()), tool.description(), tool.inputSchema()))
                .toList();
        LlmChatResponse reply = llmClient.chat(modelCall, messages, specs);
        if (!reply.hasToolCalls()) {
            return finish(context, node, new AgentToolLoopResult(reply.content(), AgentToolOutcome.COMPLETED,
                    agentName, catalogNames, turn.invocations()));
        }
        messages.add(LlmMessage.assistant(reply.content(), reply.toolCalls()));

        String runId = runId(context);
        String scope = (context.getGraph() != null ? context.getGraph().getId() : "") + "/" + node.getId();
        List<ToolInvocationRecord> invocations = new ArrayList<>(turn.invocations());
        List<RecordedCall> recorded = new ArrayList<>();
        int index = 0;
        for (LlmToolCall call : reply.toolCalls()) {
            index++;
            Optional<String> toolGateName = names.toolGateName(call.name());
            if (toolGateName.isEmpty()) {
                invocations.add(new ToolInvocationRecord(call.name(), null, turn.round(), ToolInvocationRecord.INVALID_CALL, null));
                messages.add(LlmMessage.toolResult(call.name(),
                        "ERROR: there is no tool named '" + call.name() + "'. Use only the tools you were given."));
                continue;
            }
            if (call.arguments() == null) {
                invocations.add(new ToolInvocationRecord(toolGateName.get(), null, turn.round(), ToolInvocationRecord.INVALID_CALL, null));
                messages.add(LlmMessage.toolResult(call.name(),
                        "ERROR: the arguments must be a JSON object, got: " + truncate(call.rawArguments(), 300)));
                continue;
            }
            String operationId = ToolCallRequestIds.logicalOperationId(runId, scope, turn.pass(), turn.round(), index);
            recorded.add(new RecordedCall(operationId, call.name(), toolGateName.get(), call.arguments(), clock.millis(), null, null));
        }
        Turn next = new Turn(turn.pass(), recorded.isEmpty() ? turn.round() + 1 : turn.round(), turn.catalog(),
                messages, recorded, invocations, null);
        save(context, node, next);
        log.info("Agent node {} as '{}': round {} recorded {} operation(s)", node.getId(), agentName, turn.round(),
                recorded.size());
        return new Step(null, "recorded " + recorded.size() + " tool call(s) for round " + turn.round());
    }

    private Step dispatch(KernelRuntimeContext context, NodeDefinition node, ToolGateClient toolGate, String agentName,
                          Turn turn) {
        List<LlmMessage> messages = new ArrayList<>(turn.messages());
        List<ToolInvocationRecord> invocations = new ArrayList<>(turn.invocations());
        List<RecordedCall> calls = new ArrayList<>(turn.pending());
        long now = clock.millis();
        for (int i = 0; i < calls.size(); i++) {
            RecordedCall call = calls.get(i);
            if (call.status() != null) {
                continue;
            }
            // M5.3: past the dedup retention ToolGate may no longer know the key, so the call is never sent blindly.
            if (now - call.recordedAtUnixMs() > DEDUP_RETENTION_MS) {
                invocations.add(new ToolInvocationRecord(call.tool(), call.operationId(), turn.round(),
                        ToolInvocationRecord.OUTCOME_UNKNOWN, call.invocationId()));
                return finish(context, node, new AgentToolLoopResult(unknownMessage(call, null), AgentToolOutcome.OUTCOME_UNKNOWN,
                        agentName, names(turn.catalog()), invocations));
            }
            ToolGateCallResult result = toolGate.callTool(agentName, call.operationId(), call.tool(), call.arguments());
            if (result.pendingApproval()) {
                long since = turn.waitingSinceUnixMs() != null ? turn.waitingSinceUnixMs() : now;
                if (now - since > APPROVAL_EXPIRY_MS) {
                    invocations.add(new ToolInvocationRecord(call.tool(), call.operationId(), turn.round(),
                            ToolInvocationRecord.PENDING_APPROVAL, result.invocationId()));
                    return finish(context, node, new AgentToolLoopResult(
                            "The approval for '" + call.tool() + "' was not decided in time. Nothing was run for it.",
                            AgentToolOutcome.APPROVAL_EXPIRED, agentName, names(turn.catalog()), invocations));
                }
                // M5.5: wait durably, then resend this same logical operation; once approved, ToolGate runs this invocation.
                calls.set(i, new RecordedCall(call.operationId(), call.modelName(), call.tool(), call.arguments(),
                        call.recordedAtUnixMs(), null, result.invocationId()));
                save(context, node, new Turn(turn.pass(), turn.round(), turn.catalog(), messages, calls, invocations, since));
                context.getVariables().set(RESUME_AFTER_VARIABLE, APPROVAL_POLL_MS);
                log.info("Agent node {} as '{}': {} waits for approval (invocation {})",
                        node.getId(), agentName, call.tool(), result.invocationId());
                return new Step(null, "waiting for approval of " + call.tool(), APPROVAL_POLL_MS);
            }
            if (result.outcomeUnknown()) {
                // M5.4: it may have taken effect, so it is never sent again, only reconciled.
                invocations.add(new ToolInvocationRecord(call.tool(), call.operationId(), turn.round(),
                        ToolInvocationRecord.OUTCOME_UNKNOWN, result.invocationId()));
                return finish(context, node, new AgentToolLoopResult(
                        unknownMessage(call.done(ToolInvocationRecord.OUTCOME_UNKNOWN, result.invocationId()), reconciliation(result)),
                        AgentToolOutcome.OUTCOME_UNKNOWN, agentName, names(turn.catalog()), invocations));
            }
            String status = result.error() || result.ended() ? ToolInvocationRecord.ERROR : ToolInvocationRecord.OK;
            invocations.add(new ToolInvocationRecord(call.tool(), call.operationId(), turn.round(), status, result.invocationId()));
            messages.add(LlmMessage.toolResult(call.modelName(), forModel(result)));
            calls.set(i, call.done(status, result.invocationId()));
        }
        save(context, node, new Turn(turn.pass(), turn.round() + 1, turn.catalog(), messages, List.of(), invocations, null));
        return new Step(null, "dispatched " + turn.pending().size() + " tool call(s) for round " + turn.round());
    }

    /** M5.4: the reconciliation method ToolGate reports for an unknown outcome, or null. */
    static String reconciliation(ToolGateCallResult result) {
        JsonNode structured = result.structuredContent();
        JsonNode method = structured == null ? null : structured.get("reconciliation");
        return method != null && method.isTextual() ? method.asText() : null;
    }

    static String unknownMessage(RecordedCall call, String method) {
        String settled = switch (method == null ? "" : method) {
            case "STATUS_LOOKUP" -> " ToolGate settles it with the tool's status lookup.";
            case "DOWNSTREAM_IDEMPOTENCY" -> " ToolGate may resend it with the same idempotency key.";
            case "MANUAL" -> " An operator settles it in ToolGate.";
            default -> "";
        };
        return "The outcome of '" + call.tool() + "' is unknown"
                + (call.invocationId() != null ? " (invocation " + call.invocationId() + ")" : "")
                + ". It was not run again; it needs reconciliation before this request continues." + settled;
    }

    private static String waitingMessage(Turn turn) {
        RecordedCall waiting = turn.pending().stream().filter(c -> c.status() == null).findFirst().orElse(null);
        return waiting == null ? "Waiting for approval in ToolGate." : waitingMessage(waiting.tool(), waiting.invocationId());
    }

    private static Step finish(KernelRuntimeContext context, NodeDefinition node, AgentToolLoopResult result) {
        context.getVariables().set(TURN_VARIABLE_PREFIX + node.getId(), null);
        return new Step(result, null);
    }

    private static Turn load(KernelRuntimeContext context, NodeDefinition node) {
        String stored = context.getVariables().getString(TURN_VARIABLE_PREFIX + node.getId());
        if (stored == null) {
            return null;
        }
        try {
            return JSON.readValue(stored, Turn.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unreadable agent turn state for node " + node.getId(), e);
        }
    }

    private static void save(KernelRuntimeContext context, NodeDefinition node, Turn turn) {
        try {
            context.getVariables().set(TURN_VARIABLE_PREFIX + node.getId(), JSON.writeValueAsString(turn));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("agent turn state for node " + node.getId() + " is not serializable", e);
        }
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
