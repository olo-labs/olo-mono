// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.memory;

import io.ololabs.toolgate.contracts.ServiceAudience;

import org.olo.knowledge.core.KnowledgeServer;

public final class MemoryMain {
    public static void main(String[] args) throws Exception {
        var db = KnowledgeServer.database();
        var retention = new WorkingStateRetention(db);
        var executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        retention.purge(KnowledgeServer.env("TENANT_ID"));
                    } catch (Exception e) {
                        System.err.println("Working-state purge incomplete; retry scheduled");
                    }
                },
                1,
                60,
                java.util.concurrent.TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
        KnowledgeServer.start(ServiceAudience.olo_memory, new MemoryService(db)::handle);
    }
}
