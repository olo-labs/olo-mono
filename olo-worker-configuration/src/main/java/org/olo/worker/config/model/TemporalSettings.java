/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Temporal client connection settings for the worker process.
 */
@JsonDeserialize(builder = TemporalSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TemporalSettings {

    private final Boolean enabled;
    private final String namespace;
    private final String target;

    private TemporalSettings(Builder builder) {
        this.enabled = builder.enabled;
        this.namespace = builder.namespace;
        this.target = builder.target;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    @JsonProperty("namespace")
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty("target")
    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemporalSettings that)) {
            return false;
        }
        return Objects.equals(enabled, that.enabled)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, namespace, target);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Boolean enabled;
        private String namespace;
        private String target;

        @JsonProperty("enabled")
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @JsonProperty("namespace")
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        @JsonProperty("target")
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public TemporalSettings build() {
            return new TemporalSettings(this);
        }
    }
}
