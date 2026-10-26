// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.olo.knowledge.testing.PostgresFixture.*;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;

import java.util.*;
import java.util.concurrent.*;

/**
 * Real SQL publication/ACL tests; deterministic vector/model doubles exercise races without hiding
 * storage behavior.
 */
class RagStorageTest {
    static Database db;
    String kb = UUID.randomUUID().toString(), doc = UUID.randomUUID().toString();
    List<VectorIndex.Point> points = new CopyOnWriteArrayList<>();
    List<String> forcedCandidates;
    CountDownLatch entered, release;
    EmbeddingModel model =
            new EmbeddingModel() {
                public String version() {
                    return "test@immutable-v1";
                }

                public List<List<Double>> embed(List<String> texts) throws Exception {
                    if (entered != null) {
                        entered.countDown();
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                    }
                    return texts.stream().map(t -> List.of(1.0, 0.0)).toList();
                }
            };
    VectorIndex index =
            new VectorIndex() {
                public void put(List<Point> p) {
                    points.addAll(p);
                }

                public List<String> query(
                        String tenant,
                        String kb,
                        String model,
                        List<String> versions,
                        List<Double> vector,
                        int limit) {
                    if (forcedCandidates != null) return forcedCandidates;
                    return points.stream()
                            .filter(
                                    p ->
                                            p.tenant().equals(tenant)
                                                    && p.kb().equals(kb)
                                                    && versions.contains(
                                                            p.doc() + ":" + p.version()))
                            .limit(limit)
                            .map(Point::id)
                            .toList();
                }

                public void delete(String tenant, String kb, String doc, long version) {
                    points.removeIf(
                            p ->
                                    p.tenant().equals(tenant)
                                            && p.kb().equals(kb)
                                            && p.doc().equals(doc)
                                            && p.version() == version);
                }

                public Page scan(String tenant, String cursor) {
                    return new Page(
                            points.stream()
                                    .filter(p -> p.tenant().equals(tenant))
                                    .map(p -> new Reference(p.kb(), p.doc(), p.version()))
                                    .toList(),
                            null);
                }
            };
    RagService service;

    @BeforeAll
    static void setup() throws Exception {
        db = PostgresFixture.create("rag", true);
    }

    @BeforeEach
    void before() {
        service = new RagService(db, model, index);
    }

    Map<String, Object> document(String text, long expected) {
        return Map.of(
                "kbId",
                kb,
                "docId",
                doc,
                "create",
                true,
                "content",
                text,
                "mediaType",
                "text/plain",
                "expectedVersion",
                expected);
    }

    Object call(String tenant, String subject, String op, Object body) throws Exception {
        try (var c = db.open(tenant)) {
            var v = service.execute(c, new Scope(tenant, subject, op), json(body));
            c.commit();
            return v;
        }
    }

    @Test
    void candidateJoinPreservesRankAndRejectsPrivateAndDuplicateVectorHits() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("first public source", 0));
        String first = points.getLast().id();
        doc = UUID.randomUUID().toString();
        call(TENANT, "alice", "rag.ingest", document("private source", 0));
        String secret = points.getLast().id();
        call(
                TENANT,
                "alice",
                "rag.share",
                Map.of("kbId", kb, "docId", doc, "member", "alice", "restricted", true));
        doc = UUID.randomUUID().toString();
        call(TENANT, "alice", "rag.ingest", document("second public source", 0));
        String second = points.getLast().id();
        call(TENANT, "alice", "rag.share", Map.of("kbId", kb, "member", "bob", "write", false));
        // A faulty vector backend ignores the requested authorization filter.
        // SQL must independently exclude its private hit and retain vector rank.
        forcedCandidates = List.of(second, secret, first, second);
        var chunks =
                json(call(TENANT, "bob", "rag.query", Map.of("kbId", kb, "query", "source")))
                        .path("chunks");
        assertEquals(2, chunks.size());
        assertEquals(second, chunks.get(0).path("chunk_id").asText());
        assertEquals(first, chunks.get(1).path("chunk_id").asText());
    }

    @Test
    void concurrentReadersShareTheAuthorizationFenceWhileAnAclWriteWaits() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("shared source", 0));
        call(TENANT, "alice", "rag.share", Map.of("kbId", kb, "member", "bob", "write", false));
        entered = new CountDownLatch(2);
        release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(3);
        try {
            var first =
                    pool.submit(
                            () ->
                                    call(
                                            TENANT,
                                            "alice",
                                            "rag.query",
                                            Map.of("kbId", kb, "query", "source")));
            var second =
                    pool.submit(
                            () ->
                                    call(
                                            TENANT,
                                            "bob",
                                            "rag.query",
                                            Map.of("kbId", kb, "query", "source")));
            assertTrue(
                    entered.await(2, TimeUnit.SECONDS), "Reads were serialized before embedding");
            var unshare =
                    pool.submit(
                            () ->
                                    call(
                                            TENANT,
                                            "alice",
                                            "rag.share",
                                            Map.of("kbId", kb, "member", "bob", "remove", true)));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            unshare.get(5, TimeUnit.SECONDS);
            assertThrows(
                    SecurityException.class,
                    () -> call(TENANT, "bob", "rag.query", Map.of("kbId", kb, "query", "source")));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void forcedReindexRepairsLostVectorsWithoutRevivingDeletedDocuments() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("restorable source", 0));
        points.clear();
        var request = Map.of("kbId", kb, "docId", doc, "type", "reindex", "force", true);
        var repaired = json(call(TENANT, "alice", "rag.update", request));
        assertEquals(2, repaired.path("version").asLong());
        assertFalse(points.isEmpty());
        assertFalse(
                json(call(TENANT, "alice", "rag.query", Map.of("kbId", kb, "query", "source")))
                        .path("chunks")
                        .isEmpty());
        call(TENANT, "alice", "rag.delete", Map.of("kbId", kb, "docId", doc));
        assertThrows(SecurityException.class, () -> call(TENANT, "alice", "rag.update", request));
    }

    @Test
    void unauthorizedValidIdsAndOtherTenantAreRefused() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("secret", 0));
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "bob", "rag.query", Map.of("kbId", kb, "query", "secret")));
        assertThrows(
                SecurityException.class,
                () ->
                        call(
                                OTHER_TENANT,
                                "alice",
                                "rag.query",
                                Map.of("kbId", kb, "query", "secret")));
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "bob", "rag.delete", Map.of("kbId", kb, "docId", doc)));
    }

    @Test
    void versionsPublishAtomicallyAndCitationsMatchCurrentText() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("version one", 0));
        call(TENANT, "alice", "rag.update", document("version two", 1));
        var chunks =
                json(call(TENANT, "alice", "rag.query", Map.of("kbId", kb, "query", "version")))
                        .path("chunks");
        assertEquals(1, chunks.size());
        assertEquals(2, chunks.get(0).path("version").asInt());
        assertEquals("version two", chunks.get(0).path("content").asText());
        assertTrue(chunks.get(0).path("untrusted").asBoolean());
        assertTrue(chunks.get(0).path("resource").asText().endsWith("/doc/" + doc));
        service.cleanup(TENANT);
        assertEquals(1, points.size());
    }

    @Test
    void sameVersionBodyIsIdempotentAndConflictingVersionRejected() throws Exception {
        var body = document("hello", 0);
        assertEquals(
                call(TENANT, "alice", "rag.ingest", body),
                call(TENANT, "alice", "rag.ingest", body));
        assertEquals(1, points.size());
        assertThrows(
                IllegalArgumentException.class,
                () -> call(TENANT, "alice", "rag.update", document("changed", 0)));
    }

    @Test
    void deletionImmediatelyExcludesVectorsAndCannotBeRevived() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("secret", 0));
        call(TENANT, "alice", "rag.delete", Map.of("kbId", kb, "docId", doc));
        assertFalse(points.isEmpty(), "Simulate delayed cleanup");
        assertTrue(
                json(call(TENANT, "alice", "rag.query", Map.of("kbId", kb, "query", "secret")))
                        .path("chunks")
                        .isEmpty());
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "alice", "rag.ingest", document("revive", 1)));
        service.cleanup(TENANT);
        assertTrue(points.isEmpty());
    }

    @Test
    void deleteWaitsForIndexPublicationThenWinsPermanently() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("first", 0));
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var update =
                    pool.submit(
                            () -> call(TENANT, "alice", "rag.update", document("replacement", 1)));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var delete =
                    pool.submit(
                            () ->
                                    call(
                                            TENANT,
                                            "alice",
                                            "rag.delete",
                                            Map.of("kbId", kb, "docId", doc)));
            release.countDown();
            update.get(5, TimeUnit.SECONDS);
            delete.get(5, TimeUnit.SECONDS);
        }
        entered = null;
        assertTrue(
                json(call(TENANT, "alice", "rag.query", Map.of("kbId", kb, "query", "first")))
                        .path("chunks")
                        .isEmpty());
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "alice", "rag.update", document("revive", 2)));
    }

    @Test
    void unshareInvalidatesLoadedSourcesImmediately() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("secret", 0));
        call(TENANT, "alice", "rag.share", Map.of("kbId", kb, "member", "bob", "write", false));
        call(TENANT, "bob", "rag.query", Map.of("kbId", kb, "query", "secret"));
        call(TENANT, "alice", "rag.share", Map.of("kbId", kb, "member", "bob", "remove", true));
        assertThrows(
                SecurityException.class,
                () ->
                        call(
                                TENANT,
                                "bob",
                                "rag.query",
                                Map.of(
                                        "kbId",
                                        kb,
                                        "type",
                                        "check",
                                        "sources",
                                        List.of(Map.of("docId", doc, "version", 1)))));
    }

    @Test
    void parserDoesNotFetchUrlsOrAcceptExecutableFormats() throws Exception {
        var body = new HashMap<>(document("<script>not executed</script>", 0));
        body.put("mediaType", "text/html");
        assertThrows(
                IllegalArgumentException.class, () -> call(TENANT, "alice", "rag.ingest", body));
        body.put("mediaType", "text/plain");
        body.put("content", "http://169.254.169.254/latest/meta-data");
        call(TENANT, "alice", "rag.ingest", body);
        assertEquals(1, points.size());
    }

    @Test
    void documentAclCanNarrowAndExplicitlyWidenKnowledgeBaseVisibility() throws Exception {
        call(TENANT, "alice", "rag.ingest", document("restricted secret", 0));
        call(TENANT, "alice", "rag.share", Map.of("kbId", kb, "member", "bob", "write", false));
        call(
                TENANT,
                "alice",
                "rag.share",
                Map.of("kbId", kb, "docId", doc, "member", "carol", "restricted", true));
        assertTrue(
                json(call(TENANT, "bob", "rag.query", Map.of("kbId", kb, "query", "secret")))
                        .path("chunks")
                        .isEmpty());
        assertEquals(
                1,
                json(call(TENANT, "carol", "rag.query", Map.of("kbId", kb, "query", "secret")))
                        .path("chunks")
                        .size());
        call(
                TENANT,
                "alice",
                "rag.share",
                Map.of("kbId", kb, "docId", doc, "member", "carol", "remove", true));
        assertThrows(
                SecurityException.class,
                () -> call(TENANT, "carol", "rag.query", Map.of("kbId", kb, "query", "secret")));
    }

    @Test
    void vectorsFromAFailedPublicationAreSweptWithoutRevivingOrDeletingPublishedContent()
            throws Exception {
        call(TENANT, "alice", "rag.ingest", document("published", 0));
        String orphan = UUID.randomUUID().toString();
        index.put(
                List.of(
                        new VectorIndex.Point(
                                orphan, TENANT, kb, doc, 99, model.version(), List.of(1.0, 0.0))));
        service.cleanup(TENANT);
        assertEquals(1, points.size());
        assertNotEquals(orphan, points.getFirst().id());
    }
}
