/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.olo.dto.WorkerRefreshResponse;
import com.olo.service.WorkerRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker")
public class WorkerRefreshController {

    private final WorkerRefreshService workerRefreshService;

    public WorkerRefreshController(WorkerRefreshService workerRefreshService) {
        this.workerRefreshService = workerRefreshService;
    }

    /**
     * Worker-only refresh via Redis. For full-stack refresh use POST /api/v1/system/refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<WorkerRefreshResponse> refresh() {
        String value = workerRefreshService.signalRefresh();
        return ResponseEntity.ok(new WorkerRefreshResponse(workerRefreshService.refreshKey(), value));
    }
}
