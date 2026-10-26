/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WorkerRefreshService {

    private final StringRedisTemplate redisTemplate;
    private final String refreshKey;

    public WorkerRefreshService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${olo.worker.refresh-key:olo:worker:refresh}") String refreshKey) {
        this.redisTemplate = redisTemplate;
        this.refreshKey = refreshKey != null && !refreshKey.isBlank()
                ? refreshKey.trim()
                : "olo:worker:refresh";
    }

    public String refreshKey() {
        return refreshKey;
    }

    public String signalRefresh() {
        if (redisTemplate == null) {
            throw new IllegalStateException(
                    "Redis is not enabled; cannot signal worker refresh");
        }
        try {
            String value = Instant.now().toString();
            redisTemplate.opsForValue().set(refreshKey, value);
            return value;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Redis worker refresh failed: " + e.getMessage(), e);
        }
    }
}
