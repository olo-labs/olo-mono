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
 * Cache / Redis connection settings used by the worker for input offload and shared state.
 */
@JsonDeserialize(builder = CacheSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CacheSettings {

    private final Boolean enabled;
    private final String host;
    private final Integer port;

    private CacheSettings(Builder builder) {
        this.enabled = builder.enabled;
        this.host = builder.host;
        this.port = builder.port;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    @JsonProperty("port")
    public Integer getPort() {
        return port;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheSettings that)) {
            return false;
        }
        return Objects.equals(enabled, that.enabled)
                && Objects.equals(host, that.host)
                && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, host, port);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Boolean enabled;
        private String host;
        private Integer port;

        @JsonProperty("enabled")
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @JsonProperty("host")
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        @JsonProperty("port")
        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public CacheSettings build() {
            return new CacheSettings(this);
        }
    }
}
