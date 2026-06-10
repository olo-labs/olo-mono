package org.olo.worker.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Workflow input handling limits for the worker boundary.
 */
@JsonDeserialize(builder = InputSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InputSettings {

    public static final int DEFAULT_MAX_LOCAL_MESSAGE_SIZE = 50;

    private final Integer maxLocalMessageSize;

    private InputSettings(Builder builder) {
        this.maxLocalMessageSize = builder.maxLocalMessageSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Maximum inline string size before offloading to cache storage. Defaults to {@value #DEFAULT_MAX_LOCAL_MESSAGE_SIZE}.
     */
    @JsonProperty("maxLocalMessageSize")
    public Integer getMaxLocalMessageSize() {
        return maxLocalMessageSize;
    }

    public int resolveMaxLocalMessageSize() {
        return maxLocalMessageSize == null ? DEFAULT_MAX_LOCAL_MESSAGE_SIZE : maxLocalMessageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InputSettings that)) {
            return false;
        }
        return Objects.equals(maxLocalMessageSize, that.maxLocalMessageSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxLocalMessageSize);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Integer maxLocalMessageSize;

        @JsonProperty("maxLocalMessageSize")
        public Builder maxLocalMessageSize(Integer maxLocalMessageSize) {
            this.maxLocalMessageSize = maxLocalMessageSize;
            return this;
        }

        public InputSettings build() {
            return new InputSettings(this);
        }
    }
}
