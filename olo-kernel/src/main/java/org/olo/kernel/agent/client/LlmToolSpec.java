/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * A tool offered to the model for native function calling.
 *
 * @param name        model-facing name (letters, digits, {@code _} and {@code -})
 * @param description what the tool does
 * @param parameters  JSON Schema of the arguments object
 */
public record LlmToolSpec(String name, String description, JsonNode parameters) {

    public LlmToolSpec {
        Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
    }
}
