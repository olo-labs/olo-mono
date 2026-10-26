/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.dto;

public record ModelProviderTestResponse(
        boolean ok,
        String message,
        Long latencyMs,
        String model
) {
    public static ModelProviderTestResponse success(String message, long latencyMs, String model) {
        return new ModelProviderTestResponse(true, message, latencyMs, model);
    }

    public static ModelProviderTestResponse failure(String message) {
        return new ModelProviderTestResponse(false, message, null, null);
    }
}
