/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.olo.dto.SystemRefreshResponse;
import com.olo.service.SystemRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Full-stack refresh: worker + olo runtime + instructions for studio UI reload.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemRefreshController {

    private final SystemRefreshService systemRefreshService;

    public SystemRefreshController(SystemRefreshService systemRefreshService) {
        this.systemRefreshService = systemRefreshService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<SystemRefreshResponse> refresh() {
        return ResponseEntity.ok(systemRefreshService.refreshAll());
    }
}
