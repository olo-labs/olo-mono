/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.tool;

import java.util.Map;
import java.util.Objects;

/**
 * Arguments and configuration passed to a {@link Tool} at invocation time.
 */
public record ToolRequest(
        String toolId,
        String invocationId,
        Map<String, Object> arguments,
        Map<String, Object> configuration) {

    public ToolRequest {
        Objects.requireNonNull(toolId, "toolId");
        invocationId = invocationId == null ? toolId : invocationId;
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}
