// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import java.net.URI;
import java.util.*;

/**
 * A real embedding model: startup verifies the server's installed model digest, not a mutable tag
 * alone.
 */
public final class OllamaEmbeddings implements EmbeddingModel {
    private final JsonHttp http;
    private final String model, digest;
    private final int threads;

    public OllamaEmbeddings(URI origin, String model, String digest) throws Exception {
        this(origin, model, digest, 2);
    }

    public OllamaEmbeddings(URI origin, String model, String digest, int threads) throws Exception {
        if (threads < 1 || threads > 16)
            throw new IllegalArgumentException("Embedding threads must be 1–16");
        if (!digest.matches("sha256:[a-f0-9]{64}"))
            throw new IllegalArgumentException("Pinned embedding digest required");
        this.http = new JsonHttp(origin, null);
        this.model = model;
        this.digest = digest;
        this.threads = threads;
        checkModel();
    }

    private void checkModel() throws Exception {
        // /api/show returns model details but not a trustworthy digest; /api/tags does.
        var tags = http.send("GET", "/api/tags", Map.of(), 1_048_576).path("models");
        boolean found = false;
        for (var tag : tags)
            if (model.equals(tag.path("name").asText())
                    && digest.substring(7)
                            .equals(tag.path("digest").asText().replaceFirst("^sha256:", "")))
                found = true;
        if (!found) throw new IllegalStateException("Embedding artifact digest mismatch");
    }

    @Override
    public String version() {
        return model + "@" + digest;
    }

    @Override
    public List<List<Double>> embed(List<String> texts) throws Exception {
        checkModel();
        var json =
                http.send(
                        "POST",
                        "/api/embed",
                        Map.of(
                                "model",
                                model,
                                "input",
                                texts,
                                "truncate",
                                false,
                                "keep_alive",
                                "5m",
                                "options",
                                Map.of("num_thread", threads)),
                        32_000_000);
        if (!model.equals(json.path("model").asText()))
            throw new IllegalStateException("Embedding model mismatch");
        var vectors = new ArrayList<List<Double>>();
        for (var vector : json.path("embeddings")) {
            var values = new ArrayList<Double>();
            for (var value : vector) {
                double n = value.asDouble(Double.NaN);
                if (!Double.isFinite(n)) throw new IllegalStateException("Invalid embedding");
                values.add(n);
            }
            if (values.isEmpty()) throw new IllegalStateException("Empty embedding");
            vectors.add(List.copyOf(values));
        }
        if (vectors.size() != texts.size())
            throw new IllegalStateException("Embedding count mismatch");
        checkModel();
        return List.copyOf(vectors);
    }
}
