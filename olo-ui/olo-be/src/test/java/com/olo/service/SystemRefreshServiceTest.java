/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.dto.SystemRefreshResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemRefreshServiceTest {

    @Mock
    private WorkerRefreshService workerRefreshService;

    @Mock
    private OloRuntimeRefreshClient runtimeRefreshClient;

    @InjectMocks
    private SystemRefreshService systemRefreshService;

    @Test
    void refreshAllSignalsWorkerAndRuntime() {
        when(workerRefreshService.refreshKey()).thenReturn("olo:worker:refresh");
        when(workerRefreshService.signalRefresh()).thenReturn("2026-06-16T00:00:00Z");
        when(runtimeRefreshClient.reloadConfiguration())
                .thenReturn(new OloRuntimeRefreshClient.RuntimeReloadResult(true, 1, 12, null));
        when(runtimeRefreshClient.runtimeBaseUrl()).thenReturn("http://localhost:7080");

        SystemRefreshResponse response = systemRefreshService.refreshAll();

        assertThat(response.ok()).isTrue();
        assertThat(response.runtimeReloaded()).isTrue();
        assertThat(response.workerRefreshValue()).isEqualTo("2026-06-16T00:00:00Z");
        assertThat(response.steps()).anyMatch(step -> step.startsWith("worker:"));
        assertThat(response.steps()).anyMatch(step -> step.startsWith("runtime:"));
        verify(workerRefreshService).signalRefresh();
        verify(runtimeRefreshClient).reloadConfiguration();
    }
}
