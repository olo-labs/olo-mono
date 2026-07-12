/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Shared Jackson settings for editor catalog JSON (matches annotation processor output). */
final class CatalogJsonWriter {

    private CatalogJsonWriter() {
    }

    static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configOverride(boolean.class)
                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_DEFAULT, JsonInclude.Include.NON_DEFAULT));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
