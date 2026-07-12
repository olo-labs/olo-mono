/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Jackson settings for generated catalog JSON.
 * <p>
 * Omits unset optional strings ({@code null}); keeps empty arrays at descriptor level; omits empty
 * parameter {@code values} / {@code examples}; omits boolean {@code false} (annotation defaults).
 */
public final class CatalogJsonMapper {

    private CatalogJsonMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configOverride(boolean.class)
                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_DEFAULT, JsonInclude.Include.NON_DEFAULT));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
