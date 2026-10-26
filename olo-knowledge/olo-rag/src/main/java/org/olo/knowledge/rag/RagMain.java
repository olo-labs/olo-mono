// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import io.ololabs.toolgate.contracts.ServiceAudience;

import org.olo.knowledge.core.KnowledgeServer;

import java.net.URI;
import java.util.concurrent.*;

public final class RagMain {
    public static void main(String[] args) throws Exception {
        var model =
                new OllamaEmbeddings(
                        URI.create(KnowledgeServer.env("EMBEDDING_ORIGIN")),
                        KnowledgeServer.env("EMBEDDING_MODEL"),
                        KnowledgeServer.env("EMBEDDING_DIGEST"),
                        Integer.parseInt(System.getenv().getOrDefault("EMBEDDING_THREADS", "2")));
        var index =
                new QdrantIndex(
                        URI.create(KnowledgeServer.env("QDRANT_ORIGIN")),
                        KnowledgeServer.env("QDRANT_API_KEY"),
                        KnowledgeServer.env("QDRANT_COLLECTION"));
        var service = new RagService(KnowledgeServer.database(), model, index);
        var cleanup = Executors.newSingleThreadScheduledExecutor();
        cleanup.scheduleWithFixedDelay(
                () -> {
                    try {
                        service.cleanup(KnowledgeServer.env("TENANT_ID"));
                    } catch (Exception e) {
                        System.err.println("Vector cleanup incomplete; retry scheduled");
                    }
                },
                1,
                5,
                TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(cleanup::shutdown));
        KnowledgeServer.start(ServiceAudience.olo_rag, service::handle);
    }
}
