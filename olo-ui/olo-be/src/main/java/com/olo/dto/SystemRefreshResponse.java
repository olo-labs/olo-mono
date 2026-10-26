/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.dto;

import java.util.List;

public record SystemRefreshResponse(
        boolean ok,
        String workerRefreshKey,
        String workerRefreshValue,
        boolean runtimeReloaded,
        String runtimeMessage,
        List<String> steps) {

    public static SystemRefreshResponse of(
            boolean ok,
            String workerRefreshKey,
            String workerRefreshValue,
            boolean runtimeReloaded,
            String runtimeMessage,
            List<String> steps) {
        return new SystemRefreshResponse(
                ok,
                workerRefreshKey,
                workerRefreshValue,
                runtimeReloaded,
                runtimeMessage,
                List.copyOf(steps));
    }
}
