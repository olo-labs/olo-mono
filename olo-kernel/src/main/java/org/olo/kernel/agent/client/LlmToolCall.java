/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * A tool call requested by the model.
 *
 * @param name      model-facing tool name
 * @param arguments arguments object, or {@code null} when the model sent something that is not a JSON object
 * @param rawArguments the arguments exactly as the model sent them, for error messages
 */
public record LlmToolCall(String name, JsonNode arguments, String rawArguments) {

    public LlmToolCall {
        Objects.requireNonNull(name, "name");
        rawArguments = rawArguments == null ? "" : rawArguments;
    }
}
