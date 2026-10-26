// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.UUID;

public final class Json {
    public static final ObjectMapper MAPPER =
            JsonMapper.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .build();

    static {
        MAPPER.getFactory()
                .setStreamReadConstraints(
                        StreamReadConstraints.builder()
                                .maxNestingDepth(32)
                                .maxStringLength(20_971_520)
                                .build());
    }

    private Json() {}

    public static String text(JsonNode n, String key, int max) {
        var value = n.get(key);
        if (value == null
                || !value.isTextual()
                || value.textValue().isBlank()
                || value.textValue().length() > max)
            throw new IllegalArgumentException("Invalid " + key);
        return value.textValue();
    }

    public static String id(JsonNode n, String key) {
        return uuid(text(n, key, 36));
    }

    public static String uuid(String value) {
        var id = UUID.fromString(value);
        if (!id.toString().equals(value))
            throw new IllegalArgumentException("Canonical UUID required");
        return value;
    }

    public static String encode(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
}
