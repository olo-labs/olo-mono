// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.olo.knowledge.testing.PostgresFixture.*;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;

import java.net.URI;
import java.util.*;

/** Executed by tools/test_services.py --with-vectors with owned real Qdrant/Ollama containers. */
class RealVectorTest {
    @Test
    void realEmbeddingAndVectorSearchPreserveTenantScopeAndPublishedVersion() throws Exception {
        Assumptions.assumeTrue(
                System.getenv("KNOWLEDGE_QDRANT") != null,
                "Run --with-vectors for real dependency qualification");
        URI qdrant = URI.create(System.getenv("KNOWLEDGE_QDRANT")),
                ollama = URI.create(System.getenv("KNOWLEDGE_OLLAMA"));
        var model =
                new OllamaEmbeddings(
                        ollama, "all-minilm:latest", System.getenv("KNOWLEDGE_MODEL_DIGEST"));
        int dimensions = model.embed(List.of("embedding dimension probe")).getFirst().size();
        String collection = "qualification_" + UUID.randomUUID().toString().replace("-", "");
        new JsonHttp(qdrant, null)
                .send(
                        "PUT",
                        "/collections/" + collection,
                        Map.of("vectors", Map.of("size", dimensions, "distance", "Cosine")),
                        65536);
        var index = new QdrantIndex(qdrant, null, collection);
        var db = PostgresFixture.create("real_rag", true);
        var service = new RagService(db, model, index);
        String kb = UUID.randomUUID().toString(), doc = UUID.randomUUID().toString();
        var body =
                new HashMap<String, Object>(
                        Map.of(
                                "kbId",
                                kb,
                                "docId",
                                doc,
                                "create",
                                true,
                                "content",
                                "The fictional Atlas project launches in November.",
                                "mediaType",
                                "text/plain",
                                "expectedVersion",
                                0));
        try (var c = db.open(TENANT)) {
            service.execute(c, new Scope(TENANT, "alice", "rag.ingest"), json(body));
            c.commit();
        }
        body.put("content", "The fictional Atlas project launches in December.");
        body.put("expectedVersion", 1);
        try (var c = db.open(TENANT)) {
            service.execute(c, new Scope(TENANT, "alice", "rag.update"), json(body));
            c.commit();
        }
        // A near-duplicate secret vector from another tenant must never enter the query candidate
        // set.
        index.put(
                List.of(
                        new VectorIndex.Point(
                                UUID.randomUUID().toString(),
                                OTHER_TENANT,
                                kb,
                                doc,
                                2,
                                model.version(),
                                model.embed(List.of("Atlas launch date December")).getFirst())));
        try (var c = db.open(TENANT)) {
            var result =
                    json(
                            service.execute(
                                    c,
                                    new Scope(TENANT, "alice", "rag.query"),
                                    json(Map.of("kbId", kb, "query", "When does Atlas launch?"))));
            assertEquals(1, result.path("chunks").size());
            assertEquals(2, result.path("chunks").get(0).path("version").asInt());
            assertTrue(result.path("chunks").get(0).path("content").asText().contains("December"));
            c.commit();
        }
        try (var c = db.open(TENANT)) {
            service.execute(
                    c,
                    new Scope(TENANT, "alice", "rag.delete"),
                    json(Map.of("kbId", kb, "docId", doc)));
            c.commit();
        }
        service.cleanup(TENANT);
        try (var c = db.open(TENANT)) {
            assertTrue(
                    json(service.execute(
                                    c,
                                    new Scope(TENANT, "alice", "rag.query"),
                                    json(Map.of("kbId", kb, "query", "Atlas"))))
                            .path("chunks")
                            .isEmpty());
            c.commit();
        }
        assertThrows(
                IllegalStateException.class,
                () ->
                        new OllamaEmbeddings(
                                ollama, "all-minilm:latest", "sha256:" + "0".repeat(64)));
    }
}
