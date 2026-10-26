// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.memory;

import static org.junit.jupiter.api.Assertions.*;
import static org.olo.knowledge.testing.PostgresFixture.*;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;

import java.util.*;
import java.util.concurrent.*;

class MemoryStorageTest {
    static Database db;
    static MemoryService service;
    String id = UUID.randomUUID().toString(), turn = UUID.randomUUID().toString();

    @BeforeAll
    static void setup() throws Exception {
        db = PostgresFixture.create("memory", false);
        service = new MemoryService(db);
    }

    Map<String, Object> message(String mid, String role, String content) {
        return Map.of(
                "conversationId",
                id,
                "create",
                true,
                "messageId",
                mid,
                "turnId",
                turn,
                "role",
                role,
                "content",
                content,
                "dependencies",
                List.of());
    }

    Object call(String tenant, String user, String op, Object body) throws Exception {
        try (var c = db.open(tenant)) {
            var value = service.execute(c, new Scope(tenant, user, op), json(body));
            c.commit();
            return value;
        }
    }

    @Test
    void tenantAndOwnerIsolationWithValidIds() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "secret"));
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "bob", "memory.read", Map.of("conversationId", id)));
        assertThrows(
                SecurityException.class,
                () -> call(OTHER_TENANT, "alice", "memory.read", Map.of("conversationId", id)));
        try (var c = db.open(OTHER_TENANT)) {
            assertEquals(
                    0,
                    Database.rows(c, "SELECT * FROM knowledge_resource").size(),
                    "Forced RLS hides other tenants even without a query filter");
        }
    }

    @Test
    void duplicateMessageReturnsOriginalSequenceAndRejectsChangedBody() throws Exception {
        String mid = UUID.randomUUID().toString();
        var body = message(mid, "user", "hello");
        assertEquals(
                call(TENANT, "alice", "memory.append", body),
                call(TENANT, "alice", "memory.append", body));
        assertThrows(
                IllegalArgumentException.class,
                () -> call(TENANT, "alice", "memory.append", message(mid, "user", "changed")));
    }

    @Test
    void currentMessageExcludedAndBudgetBounded() throws Exception {
        String first = UUID.randomUUID().toString(), current = UUID.randomUUID().toString();
        call(TENANT, "alice", "memory.append", message(first, "user", "before"));
        turn = UUID.randomUUID().toString();
        call(TENANT, "alice", "memory.append", message(current, "user", "current"));
        var result =
                json(
                        call(
                                TENANT,
                                "alice",
                                "memory.read",
                                Map.of("conversationId", id, "beforeMessageId", current)));
        assertEquals(1, result.path("messages").size());
        assertEquals("before", result.path("messages").get(0).path("content").asText());
        assertEquals(
                0,
                json(call(
                                TENANT,
                                "alice",
                                "memory.read",
                                Map.of("conversationId", id, "budget", 2)))
                        .path("messages")
                        .size());
    }

    @Test
    void shareAndImmediateRevocation() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "secret"));
        call(
                TENANT,
                "alice",
                "memory.share",
                Map.of("conversationId", id, "member", "bob", "write", false));
        call(TENANT, "bob", "memory.read", Map.of("conversationId", id));
        assertThrows(
                SecurityException.class,
                () ->
                        call(
                                TENANT,
                                "bob",
                                "memory.append",
                                message(UUID.randomUUID().toString(), "user", "attempt")));
        call(
                TENANT,
                "alice",
                "memory.share",
                Map.of("conversationId", id, "member", "bob", "remove", true));
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "bob", "memory.read", Map.of("conversationId", id)));
    }

    @Test
    void concurrentAppendsHaveUniqueOrderedSequences() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "first"));
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<Object>>();
            for (int i = 0; i < 12; i++) {
                String mid = UUID.randomUUID().toString();
                futures.add(
                        pool.submit(
                                () ->
                                        call(
                                                TENANT,
                                                "alice",
                                                "memory.append",
                                                message(mid, "user", "parallel"))));
            }
            for (var f : futures) f.get(10, TimeUnit.SECONDS);
        }
        try (var c = db.open(TENANT)) {
            var rows =
                    Database.rows(
                            c,
                            "SELECT sequence FROM memory_message WHERE tenant=? AND"
                                    + " conversation_id=?::uuid ORDER BY sequence",
                            TENANT,
                            id);
            assertEquals(13, rows.size());
            for (int i = 0; i < 13; i++) assertEquals((long) i + 1, rows.get(i).get("sequence"));
        }
    }

    @Test
    void deleteIsPermanentAndRemovesDerivedState() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "secret"));
        call(TENANT, "alice", "memory.delete", Map.of("conversationId", id));
        assertThrows(
                SecurityException.class,
                () ->
                        call(
                                TENANT,
                                "alice",
                                "memory.append",
                                message(UUID.randomUUID().toString(), "user", "revive")));
        try (var c = db.open(TENANT)) {
            assertTrue(
                    Database.rows(
                                    c,
                                    "SELECT * FROM memory_message WHERE tenant=? AND"
                                            + " conversation_id=?::uuid",
                                    TENANT,
                                    id)
                            .isEmpty());
        }
    }

    @Test
    void onlyOneFinalOutcomeCanBeRecorded() throws Exception {
        call(TENANT, "alice", "memory.append", message(UUID.randomUUID().toString(), "user", "hi"));
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "assistant", "done"));
        assertTrue(
                json(call(
                                TENANT,
                                "alice",
                                "memory.append",
                                message(UUID.randomUUID().toString(), "failure", "failed")))
                        .path("alreadyFinal")
                        .asBoolean());
        try (var c = db.open(TENANT)) {
            assertEquals(
                    1,
                    Database.rows(
                                    c,
                                    "SELECT 1 FROM memory_message WHERE tenant=? AND"
                                            + " conversation_id=?::uuid AND role IN"
                                            + " ('assistant','failure','cancellation')",
                                    TENANT,
                                    id)
                            .size());
        }
    }

    @Test
    void boundedWorkingStateUsesOptimisticVersion() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "hello"));
        var b =
                new HashMap<String, Object>(
                        Map.of(
                                "conversationId",
                                id,
                                "type",
                                "working",
                                "stateKey",
                                UUID.randomUUID().toString(),
                                "expectedVersion",
                                0,
                                "ttlSeconds",
                                60,
                                "value",
                                Map.of("x", 1)));
        assertEquals(1, json(call(TENANT, "alice", "memory.append", b)).path("version").asInt());
        assertThrows(
                IllegalArgumentException.class, () -> call(TENANT, "alice", "memory.append", b));
        b.put("ttlSeconds", 86401);
        assertThrows(
                IllegalArgumentException.class, () -> call(TENANT, "alice", "memory.append", b));
    }

    @Test
    void summariesAreVersionedAndInvalidatedWhenMembershipChanges() throws Exception {
        call(
                TENANT,
                "alice",
                "memory.append",
                message(UUID.randomUUID().toString(), "user", "earlier"));
        var first = json(call(TENANT, "alice", "memory.read", Map.of("conversationId", id)));
        assertEquals(1, first.path("summary").path("version").asLong());
        assertEquals(1, first.path("summary").path("dependencies").path("messages").size());
        call(TENANT, "alice", "memory.share", Map.of("conversationId", id, "member", "bob"));
        try (var c = db.open(TENANT)) {
            assertTrue(
                    Database.rows(
                                    c,
                                    "SELECT * FROM memory_summary WHERE tenant=? AND"
                                            + " conversation_id=?::uuid",
                                    TENANT,
                                    id)
                            .isEmpty());
        }
        var rebuilt = json(call(TENANT, "alice", "memory.read", Map.of("conversationId", id)));
        assertEquals(
                2,
                rebuilt.path("summary")
                        .path("dependencies")
                        .path("conversation")
                        .path("aclVersion")
                        .asLong());
    }
}
