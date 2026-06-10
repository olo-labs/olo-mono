package org.olo.annotation.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Jackson settings for generated catalog JSON.
 * <p>
 * Omits unset optional strings ({@code null}); keeps empty arrays at descriptor level; omits empty
 * property {@code enumValues} / {@code examples}; omits boolean {@code false} (annotation defaults).
 */
final class CatalogJsonMapper {

    private CatalogJsonMapper() {
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
