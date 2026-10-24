// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.*;
import org.olo.input.model.*;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.toolgate.client.*;

import java.util.*;

class ConversationLifecycleTest {
    static final ObjectMapper JSON = new ObjectMapper();
    String conversation = UUID.randomUUID().toString(), run = UUID.randomUUID().toString();
    Fake gateway = new Fake();
    ConversationLifecycle lifecycle = new ConversationLifecycle(new GovernedCalls(gateway));

    KernelRuntimeContext context() {
        var graph =
                org.olo.definition.workflow.WorkflowBuilder.create("workflow-a")
                        .id("workflow-a")
                        .queue("queue")
                        .build();
        var identity = new IdentityBinding("tenant", "alice", "issuer", "session", 1);
        var input =
                new WorkflowInput(
                        "1",
                        List.of(
                                new InputItem(
                                        "userQuery",
                                        "Query",
                                        InputType.STRING,
                                        new Storage(StorageMode.LOCAL, null, null),
                                        "current message")),
                        new Context(
                                "tenant",
                                null,
                                List.of(),
                                List.of(),
                                conversation,
                                run,
                                null,
                                null,
                                identity),
                        null,
                        null,
                        null);
        return new KernelRuntimeContext(
                "queue", input, graph, true, WorkflowRuntimeVariables.fromMap(Map.of()));
    }

    class Fake implements ToolGateClient {
        List<String> operations = new ArrayList<>(), readIds = new ArrayList<>();
        Map<String, JsonNode> messages = new LinkedHashMap<>();
        boolean revoked, failAppend, pending;

        @Override
        public ToolGateClient forUser(UserBinding binding) {
            assertEquals("alice", binding.userId());
            return this;
        }

        @Override
        public ToolGateClient forUserViaAgent(UserBinding binding) {
            fail("Path A must never use a delegated actor");
            return this;
        }

        @Override
        public List<ToolGateTool> listTools(String agent) {
            fail("No agent catalog for platform calls");
            return List.of();
        }

        @Override
        public void probe() {}

        @Override
        public ToolGateCallResult callTool(String route, String id, String tool, JsonNode args) {
            assertEquals("platform", route);
            operations.add(tool);
            if (pending)
                return new ToolGateCallResult(
                        true, "", null, "PENDING_APPROVAL", "original-invocation");
            if (revoked) throw new SecurityException("revoked");
            try {
                var body = JSON.readTree(args.path("request").asText());
                if (tool.equals("olo.memory/append")) {
                    if (failAppend && body.path("role").asText().equals("assistant"))
                        throw new IllegalStateException("storage unavailable");
                    messages.putIfAbsent(body.path("messageId").asText(), body);
                    return ok(Map.of("recorded", true));
                }
                if (tool.equals("olo.rag/query"))
                    return ok(
                            Map.of("valid", revoked ? List.of() : List.of("doc"), "aclVersion", 1));
                readIds.add(id);
                return ok(
                        Map.of(
                                "messages",
                                List.of(
                                        Map.of(
                                                "role",
                                                "user",
                                                "content",
                                                "earlier message",
                                                "dependencies",
                                                List.of())),
                                "source",
                                Map.of(
                                        "resource",
                                        "olo://tenant/conversation/" + conversation,
                                        "aclVersion",
                                        1)));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }

        ToolGateCallResult ok(Object v) {
            var value = JSON.valueToTree(v);
            ((com.fasterxml.jackson.databind.node.ObjectNode) value)
                    .put("authorizedAt", System.currentTimeMillis());
            return new ToolGateCallResult(false, "", value, null, null);
        }
    }

    @Test
    void acceptedAndFinalMessagesAreIdempotentAcrossLostActivityResults() {
        var first = context();
        lifecycle.begin(first);
        lifecycle.finish(first, "final response");
        var retry = context();
        lifecycle.begin(retry);
        lifecycle.finish(retry, "final response");
        assertEquals(2, gateway.messages.size());
        assertEquals(
                Set.of("user", "assistant"),
                gateway.messages.values().stream()
                        .map(n -> n.path("role").asText())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void historyExcludesCurrentMessageAndCurrentPromptContainsItOnce() {
        var ctx = context();
        lifecycle.begin(ctx);
        var prompt = lifecycle.prompt(ctx, "current message");
        assertEquals(1, prompt.split("current message", -1).length - 1);
        assertTrue(prompt.contains("earlier message"));
        assertTrue(prompt.contains("untrusted"));
    }

    @Test
    void finalCannotBeReleasedBeforePersistenceAndRetryKeepsSameMessageId() {
        var ctx = context();
        lifecycle.begin(ctx);
        gateway.failAppend = true;
        assertThrows(IllegalStateException.class, () -> lifecycle.finish(ctx, "final"));
        assertEquals(1, gateway.messages.size());
        gateway.failAppend = false;
        lifecycle.finish(ctx, "final");
        assertEquals(2, gateway.messages.size());
    }

    @Test
    void revocationStopsTheNextModelAndFinalRelease() {
        var ctx = context();
        lifecycle.begin(ctx);
        gateway.revoked = true;
        assertThrows(SecurityException.class, () -> lifecycle.recheck(ctx, "model-1"));
        assertThrows(SecurityException.class, () -> lifecycle.finish(ctx, "private answer"));
        assertEquals(1, gateway.messages.size());
    }

    @Test
    void eachAuthorizationCheckpointGetsAFreshReadInvocation() {
        var ctx = context();
        lifecycle.begin(ctx);
        lifecycle.recheck(ctx, "model-attempt-1");
        lifecycle.recheck(ctx, "model-attempt-2");
        assertEquals(3, new HashSet<>(gateway.readIds).size());
    }

    @Test
    void pendingApprovalRetainsTheOriginalInvocationId() {
        var calls = new GovernedCalls(gateway);
        var user = new UserBinding("tenant", "alice", "issuer", "session", 1);
        gateway.pending = true;
        assertThrows(
                GovernedCalls.Pending.class,
                () ->
                        calls.user(
                                user,
                                "original",
                                "memory.append",
                                conversation,
                                Map.of("content", "accepted")));
        assertEquals(
                "platform-"
                        + UUID.nameUUIDFromBytes(
                                "original".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                GovernedCalls.stableId("original"));
    }
}
