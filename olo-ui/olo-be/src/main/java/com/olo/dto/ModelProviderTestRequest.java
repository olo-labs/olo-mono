/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.dto;

import java.util.Map;

public record ModelProviderTestRequest(
        String provider,
        String model,
        Map<String, Object> configuration
) {
}
