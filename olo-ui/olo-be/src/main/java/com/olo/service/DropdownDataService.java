/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.dto.TenantDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DropdownDataService {

    private static final Logger log = LoggerFactory.getLogger(DropdownDataService.class);
    private static final TypeReference<List<TenantDto>> TENANT_LIST_TYPE = new TypeReference<>() {};

    private final RedisTemplate<String, String> redisTemplate;
    /** Redis key from env OLO_TENANT_IDS (e.g. olo:tenants). */
    private final String tenantIdsKey;
    private final ObjectMapper objectMapper;

    /**
     * In-memory tenant store used when Redis is disabled. Improves first-run and OSS contributor experience.
     * Data is lost on restart. Thread-safe.
     */
    private final Map<String, TenantDto> inMemoryTenants = new ConcurrentHashMap<>();

    public DropdownDataService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            @Value("${olo.tenant.ids:olo:tenants}") String tenantIdsKey,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.tenantIdsKey = tenantIdsKey != null && !tenantIdsKey.isBlank() ? tenantIdsKey.trim() : "olo:tenants";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        log.info("DropdownDataService initialized: redisTemplate={}, tenantIdsKey={} (from env OLO_TENANT_IDS). When Redis is disabled, in-memory tenant store is used (data lost on restart).",
                redisTemplate != null ? "present" : "null", this.tenantIdsKey);
    }

    /**
     * Tenants from Redis key (env OLO_TENANT_IDS), or from in-memory store when Redis is disabled.
     * In-memory fallback allows first-run and contribution without Redis.
     */
    public List<TenantDto> getTenants() {
        log.debug("getTenants() called");
        if (redisTemplate != null) {
            try {
                log.debug("getTenants: reading from Redis key={}", tenantIdsKey);
                String json = redisTemplate.opsForValue().get(tenantIdsKey);
                if (json != null && !json.isBlank()) {
                    List<TenantDto> list = objectMapper.readValue(json, TENANT_LIST_TYPE);
                    if (list != null && !list.isEmpty()) {
                        log.info("getTenants: found {} tenants from Redis key '{}'", list.size(), tenantIdsKey);
                        return list;
                    }
                }
                log.debug("getTenants: Redis key '{}' is empty or missing. Returning empty list.", tenantIdsKey);
                return Collections.emptyList();
            } catch (Exception e) {
                log.warn("getTenants: Redis/JSON error for key '{}': {}. Returning empty list.", tenantIdsKey, e.getMessage(), e);
                return Collections.emptyList();
            }
        }
        // In-memory fallback when Redis is disabled
        List<TenantDto> list = new ArrayList<>(inMemoryTenants.values());
        log.debug("getTenants: in-memory fallback, {} tenants", list.size());
        return list;
    }

    /**
     * Save tenant (add or update by id). Uses Redis when available; otherwise in-memory store.
     */
    public TenantDto saveTenant(TenantDto tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) {
            throw new IllegalArgumentException("Tenant id is required.");
        }
        if (redisTemplate != null) {
            List<TenantDto> list = new ArrayList<>(getTenants());
            List<TenantDto> updated = list.stream()
                    .filter(t -> !tenant.getId().equals(t.getId()))
                    .collect(Collectors.toList());
            updated.add(tenant);
            try {
                String json = objectMapper.writeValueAsString(updated);
                redisTemplate.opsForValue().set(tenantIdsKey, json);
                log.info("saveTenant: saved tenant id={} to Redis key '{}'", tenant.getId(), tenantIdsKey);
                return tenant;
            } catch (Exception e) {
                log.error("saveTenant: failed to write Redis key '{}'", tenantIdsKey, e);
                throw new RuntimeException("Failed to save tenant", e);
            }
        }
        // In-memory fallback
        inMemoryTenants.put(tenant.getId(), tenant);
        log.info("saveTenant: saved tenant id={} to in-memory store (Redis disabled)", tenant.getId());
        return tenant;
    }

    /**
     * Delete tenant by id. Uses Redis when available; otherwise in-memory store.
     */
    public void deleteTenant(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tenant id is required.");
        }
        if (redisTemplate != null) {
            List<TenantDto> list = new ArrayList<>(getTenants());
            List<TenantDto> updated = list.stream()
                    .filter(t -> !id.equals(t.getId()))
                    .collect(Collectors.toList());
            try {
                String json = objectMapper.writeValueAsString(updated);
                redisTemplate.opsForValue().set(tenantIdsKey, json);
                log.info("deleteTenant: removed tenant id={} from Redis key '{}'", id, tenantIdsKey);
            } catch (Exception e) {
                log.error("deleteTenant: failed to write Redis key '{}'", tenantIdsKey, e);
                throw new RuntimeException("Failed to delete tenant", e);
            }
            return;
        }
        // In-memory fallback
        inMemoryTenants.remove(id);
        log.info("deleteTenant: removed tenant id={} from in-memory store (Redis disabled)", id);
    }

    /**
     * Environments for dropdown (e.g. per tenant). Placeholder for dedicated API.
     */
    public List<String> getEnvironments(String tenantId) {
        // TODO: dedicated API / Redis or DB
        return List.of("dev", "staging", "prod");
    }

    /**
     * Run IDs for dropdown. Placeholder for dedicated API.
     */
    public List<String> getRunIds(String tenantId, String environment) {
        // TODO: dedicated API
        return Collections.emptyList();
    }
}
