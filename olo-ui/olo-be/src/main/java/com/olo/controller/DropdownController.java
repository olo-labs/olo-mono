/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.olo.dto.TenantDto;
import com.olo.service.DropdownDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DropdownController {

    private static final Logger log = LoggerFactory.getLogger(DropdownController.class);

    private final DropdownDataService dropdownDataService;

    public DropdownController(DropdownDataService dropdownDataService) {
        this.dropdownDataService = dropdownDataService;
    }

    /**
     * Tenants from Redis key olo:tenants (JSON array of { id, name }). Display: name if non-empty, else id.
     */
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantDto>> getTenants() {
        log.debug("GET /api/v1/tenants requested");
        List<TenantDto> tenants = dropdownDataService.getTenants();
        log.info("GET /api/v1/tenants returning {} tenants", tenants.size());
        return ResponseEntity.ok(tenants);
    }

    /**
     * Add or update tenant (id, name, description, configVersion). Writes to Redis key from OLO_TENANT_IDS.
     */
    @PostMapping("/tenants")
    public ResponseEntity<TenantDto> createTenant(@RequestBody TenantDto tenant) {
        log.debug("POST /api/v1/tenants requested: id={}", tenant != null ? tenant.getId() : null);
        TenantDto saved = dropdownDataService.saveTenant(tenant);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update tenant by id. Body may include name, description, configVersion; id in path must match.
     */
    @PutMapping("/tenants/{id}")
    public ResponseEntity<TenantDto> updateTenant(@PathVariable String id, @RequestBody TenantDto tenant) {
        log.debug("PUT /api/v1/tenants/{} requested", id);
        if (tenant == null) tenant = new TenantDto();
        tenant.setId(id);
        TenantDto saved = dropdownDataService.saveTenant(tenant);
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete tenant by id. Removes from Redis list.
     */
    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String id) {
        log.debug("DELETE /api/v1/tenants/{} requested", id);
        dropdownDataService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Environments for a tenant (dedicated API for dropdown details).
     */
    @GetMapping("/tenants/{tenantId}/environments")
    public ResponseEntity<List<String>> getEnvironments(@PathVariable String tenantId) {
        return ResponseEntity.ok(dropdownDataService.getEnvironments(tenantId));
    }

    /**
     * Run IDs for tenant + environment (dedicated API for dropdown details).
     */
    @GetMapping("/tenants/{tenantId}/environments/{environment}/runs")
    public ResponseEntity<List<String>> getRunIds(
            @PathVariable String tenantId,
            @PathVariable String environment) {
        return ResponseEntity.ok(dropdownDataService.getRunIds(tenantId, environment));
    }

    /**
     * Combined dropdown details (tenant, env, run) in one call if needed.
     */
    @GetMapping("/dropdown-details")
    public ResponseEntity<Map<String, Object>> getDropdownDetails(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String environment) {
        List<TenantDto> tenants = dropdownDataService.getTenants();
        List<String> environments = tenantId != null && !tenantId.isBlank()
                ? dropdownDataService.getEnvironments(tenantId) : List.of();
        List<String> runIds = (tenantId != null && environment != null && !tenantId.isBlank() && !environment.isBlank())
                ? dropdownDataService.getRunIds(tenantId, environment) : List.of();
        return ResponseEntity.ok(Map.of(
                "tenants", tenants,
                "environments", environments,
                "runIds", runIds
        ));
    }
}
