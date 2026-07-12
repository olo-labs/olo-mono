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
 * HTTP or management server binding for the worker process.
 */
@JsonDeserialize(builder = ServerSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ServerSettings {

    private final String host;
    private final Integer port;

    private ServerSettings(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    @JsonProperty("port")
    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerSettings that)) {
            return false;
        }
        return Objects.equals(host, that.host) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String host;
        private Integer port;

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

        public ServerSettings build() {
            return new ServerSettings(this);
        }
    }
}
