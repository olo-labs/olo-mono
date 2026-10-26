// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.olo.knowledge.testing.PostgresFixture.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;

import java.net.URI;
import java.nio.file.*;
import java.util.*;

/**
 * Real retrieval quality evaluation. Service-component latency is explicitly not Gateway latency.
 */
class RagEvaluationTest {
    @Test
    void evaluateTheVersionedAuthorizedCorpus() throws Exception {
        Assumptions.assumeTrue(
                "true".equals(System.getenv("KNOWLEDGE_EVALUATION")), "Use --evaluation");
        var fixture =
                Json.MAPPER.readTree(
                        Files.readString(Path.of("../fixtures/rag-evaluation-v1.json")));
        assertEquals(200, fixture.path("documents").size());
        assertEquals(150, fixture.path("queries").size());
        var model =
                new OllamaEmbeddings(
                        URI.create(System.getenv("KNOWLEDGE_OLLAMA")),
                        "all-minilm:latest",
                        System.getenv("KNOWLEDGE_MODEL_DIGEST"),
                        2);
        var origin = URI.create(System.getenv("KNOWLEDGE_QDRANT"));
        String collection = "evaluation_" + UUID.randomUUID().toString().replace("-", "");
        new JsonHttp(origin, null)
                .send(
                        "PUT",
                        "/collections/" + collection,
                        Map.of(
                                "vectors",
                                Map.of(
                                        "size",
                                        model.embed(List.of("dimension probe")).getFirst().size(),
                                        "distance",
                                        "Cosine")),
                        65536);
        var db = PostgresFixture.create("evaluation", true);
        var service = new RagService(db, model, new QdrantIndex(origin, null, collection));
        var documents = new HashMap<String, JsonNode>();
        var created = new HashSet<String>();
        for (var doc : fixture.path("documents")) {
            String tenant = doc.path("tenant").asText(),
                    kb = doc.path("kbId").asText(),
                    owner = doc.path("owner").asText();
            documents.put(doc.path("id").asText(), doc);
            try (var c = db.open(tenant)) {
                service.execute(
                        c,
                        new Scope(tenant, owner, "rag.ingest"),
                        json(
                                Map.of(
                                        "kbId",
                                        kb,
                                        "docId",
                                        doc.path("id").asText(),
                                        "content",
                                        doc.path("content").asText(),
                                        "mediaType",
                                        "text/plain",
                                        "expectedVersion",
                                        0,
                                        "create",
                                        true)));
                if (created.add(tenant)) {
                    String reader = owner.replace("-a", "-b");
                    service.execute(
                            c,
                            new Scope(tenant, owner, "rag.share"),
                            json(Map.of("kbId", kb, "member", reader, "write", false)));
                }
                if (doc.path("readers").size() == 1)
                    service.execute(
                            c,
                            new Scope(tenant, owner, "rag.share"),
                            json(
                                    Map.of(
                                            "kbId",
                                            kb,
                                            "docId",
                                            doc.path("id").asText(),
                                            "member",
                                            owner,
                                            "restricted",
                                            true)));
                c.commit();
            }
        }
        double recall = 0, precision = 0;
        int leakage = 0, citations = 0, badCitations = 0;
        var latencies = new ArrayList<Double>();
        var observations = new ArrayList<Map<String, Object>>();
        for (var query : fixture.path("queries")) {
            var relevant = new HashSet<String>();
            query.path("relevantDocumentIds").forEach(v -> relevant.add(v.asText()));
            long began = System.nanoTime();
            JsonNode result;
            try (var c = db.open(query.path("tenant").asText())) {
                result =
                        json(
                                service.execute(
                                        c,
                                        new Scope(
                                                query.path("tenant").asText(),
                                                query.path("user").asText(),
                                                "rag.query"),
                                        json(
                                                Map.of(
                                                        "kbId",
                                                        query.path("kbId").asText(),
                                                        "query",
                                                        query.path("question").asText()))));
                c.commit();
            }
            latencies.add((System.nanoTime() - began) / 1_000_000.0);
            int hits = 0;
            for (var chunk : result.path("chunks")) {
                String id = chunk.path("doc_id").asText();
                var doc = documents.get(id);
                citations++;
                boolean allowed =
                        doc != null
                                && doc.path("tenant").equals(query.path("tenant"))
                                && doc.path("kbId").equals(query.path("kbId"));
                if (allowed) {
                    allowed = false;
                    for (var reader : doc.path("readers"))
                        if (reader.equals(query.path("user"))) allowed = true;
                }
                if (!allowed) leakage++;
                if (doc == null
                        || !allowed
                        || chunk.path("version").asLong() != 1
                        || !doc.path("content").asText().contains(chunk.path("content").asText())
                        || !chunk.path("resource")
                                .asText()
                                .equals(
                                        "olo://"
                                                + query.path("tenant").asText()
                                                + "/kb/"
                                                + query.path("kbId").asText()
                                                + "/doc/"
                                                + id)) badCitations++;
                if (allowed && relevant.contains(id)) hits++;
            }
            double r = (double) hits / relevant.size(), p = (double) hits / 5;
            recall += r;
            precision += p;
            assertTrue(result.path("budgetUsed").asInt() <= 4000);
            observations.add(
                    Map.of(
                            "queryId",
                            query.path("id").asText(),
                            "recallAt5",
                            r,
                            "precisionAt5",
                            p));
        }
        recall /= 150;
        precision /= 150;
        latencies.sort(Double::compare);
        var report = new LinkedHashMap<String, Object>();
        report.put("fixtureVersion", 1);
        report.put("documents", 200);
        report.put("queries", 150);
        report.put("model", model.version());
        report.put("embeddingThreads", 2);
        report.put(
                "modelStorage",
                System.getenv().getOrDefault("KNOWLEDGE_MODEL_STORAGE", "unspecified"));
        report.put(
                "transport",
                "service component, real PostgreSQL/Qdrant/Ollama; excludes Gateway and mTLS");
        report.put("authorizedRecallAt5", recall);
        report.put("authorizedPrecisionAt5", precision);
        report.put("unauthorizedChunks", leakage);
        report.put("citationCount", citations);
        report.put("incorrectCitations", badCitations);
        report.put("componentQueryP50Ms", latencies.get(74));
        report.put("componentQueryP95Ms", latencies.get(142));
        report.put("observations", observations);
        var output = Path.of("../build/qualification/rag-evaluation.json");
        Files.createDirectories(output.getParent());
        Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
        assertEquals(0, leakage, "Unauthorized leakage");
        assertEquals(0, badCitations, "Citation correctness");
        assertTrue(recall >= .8, "Authorized recall@5: " + recall);
        assertTrue(precision >= .6, "Authorized precision@5: " + precision);
    }
}
