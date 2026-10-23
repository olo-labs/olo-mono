/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * One entry of an agent's ToolGate catalog ({@code tools/list}).
 *
 * @param name        ToolGate tool name, for example {@code incidents.host-status}
 * @param description human-readable description shown to the model
 * @param inputSchema JSON Schema of the tool arguments
 */
public record ToolGateTool(String name, String description, JsonNode inputSchema) {

    public ToolGateTool {
        Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
    }
}
