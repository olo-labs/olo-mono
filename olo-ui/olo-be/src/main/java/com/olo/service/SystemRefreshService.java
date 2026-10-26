/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.dto.SystemRefreshResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates an olo-restart-style refresh: worker (Redis), olo runtime (HTTP), studio reads from disk.
 */
@Service
public class SystemRefreshService {

    private final WorkerRefreshService workerRefreshService;
    private final OloRuntimeRefreshClient runtimeRefreshClient;

    public SystemRefreshService(
            WorkerRefreshService workerRefreshService,
            OloRuntimeRefreshClient runtimeRefreshClient) {
        this.workerRefreshService = workerRefreshService;
        this.runtimeRefreshClient = runtimeRefreshClient;
    }

    public SystemRefreshResponse refreshAll() {
        List<String> steps = new ArrayList<>();
        String workerKey = workerRefreshService.refreshKey();
        String workerValue = null;
        boolean workerOk = false;
        try {
            workerValue = workerRefreshService.signalRefresh();
            workerOk = true;
            steps.add("worker: signaled via Redis key " + workerKey);
        } catch (Exception e) {
            steps.add("worker: failed — " + e.getMessage());
        }

        OloRuntimeRefreshClient.RuntimeReloadResult runtime = runtimeRefreshClient.reloadConfiguration();
        if (runtime.ok()) {
            steps.add(String.format(
                    "runtime: reloaded %d workflow(s) from %s",
                    runtime.workflowCount(),
                    runtimeRefreshClient.runtimeBaseUrl()));
        } else {
            steps.add(String.format(
                    "runtime: not reloaded (%s) — is olo backend running on %s?",
                    runtime.message(),
                    runtimeRefreshClient.runtimeBaseUrl()));
        }

        steps.add("studio: reload catalog and workflows from disk in the browser");

        boolean ok = workerOk;
        return SystemRefreshResponse.of(
                ok,
                workerKey,
                workerValue,
                runtime.ok(),
                runtime.message(),
                steps);
    }
}
