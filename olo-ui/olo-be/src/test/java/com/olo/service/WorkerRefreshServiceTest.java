/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerRefreshServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void signalRefreshWritesTimestampToConfiguredKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        WorkerRefreshService service = new WorkerRefreshService(redisTemplate, "olo:worker:refresh");

        String value = service.signalRefresh();

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("olo:worker:refresh"), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).isEqualTo(value);
        assertThat(value).isNotBlank();
    }

    @Test
    void signalRefreshFailsWhenRedisDisabled() {
        WorkerRefreshService service = new WorkerRefreshService(null, "olo:worker:refresh");

        assertThatThrownBy(service::signalRefresh)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis is not enabled");
    }
}
