/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker;

import org.olo.worker.config.WorkerControlKeys;
import org.olo.worker.config.WorkerSettings;
import org.olo.worker.config.model.CacheSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * Polls a Redis key for refresh tokens written by olo-ui / olo-be.
 * When the value changes, triggers {@link WorkerBootstrap#start(boolean)} to reload workflows and queues.
 */
public final class WorkerRefreshMonitor {

    private static final Logger log = LoggerFactory.getLogger(WorkerRefreshMonitor.class);
    private static final long DEFAULT_POLL_MS = 2_000L;

    private static volatile Thread monitorThread;

    private WorkerRefreshMonitor() {
    }

    public static void start(WorkerSettings settings) {
        CacheSettings cache = settings.cache();
        if (cache == null || !cache.isEnabled()) {
            log.info("Worker refresh monitor disabled (cache.enabled is not true)");
            return;
        }

        String host = cache.getHost() != null && !cache.getHost().isBlank() ? cache.getHost() : "localhost";
        int port = cache.getPort() != null ? cache.getPort() : 6379;
        String key = resolveRefreshKey();
        long pollMs = resolvePollIntervalMs();

        stop();
        Thread thread = new Thread(
                () -> pollLoop(host, port, key, pollMs),
                "olo-worker-refresh-monitor");
        thread.setDaemon(true);
        monitorThread = thread;
        thread.start();
        log.info("Worker refresh monitor started: redis={}:{}, key={}, pollMs={}", host, port, key, pollMs);
    }

    public static void stop() {
        Thread thread = monitorThread;
        if (thread != null) {
            thread.interrupt();
            monitorThread = null;
        }
    }

    private static void pollLoop(String host, int port, String key, long pollMs) {
        String lastSeen = null;
        while (!Thread.currentThread().isInterrupted()) {
            try (Jedis jedis = new Jedis(host, port)) {
                String value = jedis.get(key);
                if (value != null && !value.equals(lastSeen)) {
                    if (lastSeen != null) {
                        log.info("Refresh signal on {} -> reloading worker configuration and Temporal queues", key);
                        WorkerBootstrap.start(true);
                    }
                    lastSeen = value;
                }
            } catch (Exception e) {
                log.warn("Worker refresh monitor poll failed: {}", e.toString());
            }

            try {
                Thread.sleep(pollMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("Worker refresh monitor stopped");
    }

    private static String resolveRefreshKey() {
        String env = System.getenv("OLO_WORKER_REFRESH_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return WorkerControlKeys.REFRESH_REDIS_KEY;
    }

    private static long resolvePollIntervalMs() {
        String env = System.getenv("OLO_WORKER_REFRESH_POLL_MS");
        if (env == null || env.isBlank()) {
            return DEFAULT_POLL_MS;
        }
        try {
            long parsed = Long.parseLong(env.trim());
            return parsed > 0 ? parsed : DEFAULT_POLL_MS;
        } catch (NumberFormatException e) {
            log.warn("Invalid OLO_WORKER_REFRESH_POLL_MS='{}'; using {}", env, DEFAULT_POLL_MS);
            return DEFAULT_POLL_MS;
        }
    }
}
