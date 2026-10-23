/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.kernel.agent.client.LlmToolCall;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaToolCallParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void objectArgumentsAreUsedAsIs() throws Exception {
        LlmToolCall call = OllamaLlmClient.parseToolCall("a", MAPPER.readTree("{\"host\":\"app-02\"}"));

        assertThat(call.arguments().path("host").asText()).isEqualTo("app-02");
    }

    @Test
    void stringifiedJsonArgumentsAreParsed() throws Exception {
        LlmToolCall call = OllamaLlmClient.parseToolCall("a", MAPPER.readTree("\"{\\\"host\\\":\\\"app-02\\\"}\""));

        assertThat(call.arguments().path("host").asText()).isEqualTo("app-02");
    }

    @Test
    void missingArgumentsBecomeAnEmptyObject() {
        LlmToolCall call = OllamaLlmClient.parseToolCall("a", null);

        assertThat(call.arguments().isObject()).isTrue();
        assertThat(call.arguments().size()).isZero();
    }

    @Test
    void nonObjectArgumentsAreRejectedButKeptForTheErrorMessage() throws Exception {
        LlmToolCall notJson = OllamaLlmClient.parseToolCall("a", MAPPER.readTree("\"host=app-02\""));
        LlmToolCall array = OllamaLlmClient.parseToolCall("a", MAPPER.readTree("[1,2]"));

        assertThat(notJson.arguments()).isNull();
        assertThat(notJson.rawArguments()).isEqualTo("host=app-02");
        assertThat(array.arguments()).isNull();
    }
}
