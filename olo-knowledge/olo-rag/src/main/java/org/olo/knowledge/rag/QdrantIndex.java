// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import java.net.URI;
import java.util.*;

/** Qdrant stores vectors/opaque metadata only; content and authorization remain in PostgreSQL. */
public final class QdrantIndex implements VectorIndex {
    private final JsonHttp http;
    private final String collection;

    public QdrantIndex(URI origin, String key, String collection) {
        if (!collection.matches("[A-Za-z0-9_-]{1,80}"))
            throw new IllegalArgumentException("Invalid collection");
        http = new JsonHttp(origin, key);
        this.collection = collection;
    }

    private static Map<String, Object> eq(String key, Object value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }

    @Override
    public void put(List<Point> points) throws Exception {
        var values =
                points.stream()
                        .map(
                                p ->
                                        Map.of(
                                                "id",
                                                p.id(),
                                                "vector",
                                                p.vector(),
                                                "payload",
                                                Map.of(
                                                        "tenant",
                                                        p.tenant(),
                                                        "kb",
                                                        p.kb(),
                                                        "doc",
                                                        p.doc(),
                                                        "version",
                                                        p.version(),
                                                        "docVersion",
                                                        p.doc() + ":" + p.version(),
                                                        "model",
                                                        p.model())))
                        .toList();
        http.send(
                "PUT",
                "/collections/" + collection + "/points?wait=true",
                Map.of("points", values),
                1_048_576);
    }

    @Override
    public List<String> query(
            String tenant,
            String kb,
            String model,
            List<String> versions,
            List<Double> vector,
            int limit)
            throws Exception {
        if (versions.isEmpty()) return List.of();
        var must =
                List.of(
                        eq("tenant", tenant),
                        eq("kb", kb),
                        eq("model", model),
                        Map.of("key", "docVersion", "match", Map.of("any", versions)));
        var response =
                http.send(
                        "POST",
                        "/collections/" + collection + "/points/query",
                        Map.of(
                                "query",
                                vector,
                                "filter",
                                Map.of("must", must),
                                "limit",
                                limit,
                                "with_payload",
                                false),
                        1_048_576);
        var ids = new ArrayList<String>();
        for (var p : response.path("result").path("points")) ids.add(p.path("id").asText());
        return ids;
    }

    @Override
    public void delete(String tenant, String kb, String doc, long version) throws Exception {
        http.send(
                "POST",
                "/collections/" + collection + "/points/delete?wait=true",
                Map.of(
                        "filter",
                        Map.of(
                                "must",
                                List.of(
                                        eq("tenant", tenant),
                                        eq("kb", kb),
                                        eq("doc", doc),
                                        eq("version", version)))),
                1_048_576);
    }

    @Override
    public Page scan(String tenant, String cursor) throws Exception {
        var request = new HashMap<String, Object>();
        request.put("filter", Map.of("must", List.of(eq("tenant", tenant))));
        request.put("limit", 100);
        request.put("with_payload", true);
        request.put("with_vector", false);
        if (cursor != null) request.put("offset", cursor);
        var result =
                http.send(
                                "POST",
                                "/collections/" + collection + "/points/scroll",
                                request,
                                1_048_576)
                        .path("result");
        var points = new ArrayList<Reference>();
        for (var point : result.path("points")) {
            var p = point.path("payload");
            points.add(
                    new Reference(
                            p.path("kb").asText(),
                            p.path("doc").asText(),
                            p.path("version").asLong()));
        }
        return new Page(
                points,
                result.hasNonNull("next_page_offset")
                        ? result.path("next_page_offset").asText()
                        : null);
    }
}
