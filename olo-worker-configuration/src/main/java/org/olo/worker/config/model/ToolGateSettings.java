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
 * OLO ToolGate Gateway settings. When enabled, every LLM agent asks ToolGate for the tools its agent
 * name may use and calls them through the Gateway.
 *
 * <p>Relative {@code caCertificate} and {@code credentialsDir} paths resolve against the configuration file's folder.
 */
@JsonDeserialize(builder = ToolGateSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolGateSettings {

    public static final int DEFAULT_TIMEOUT_MS = 60_000;
    public static final int DEFAULT_MAX_TOOL_ROUNDS = 8;

    private final Boolean enabled;
    private final String mcpUrl;
    private final String caCertificate;
    private final String credentialsDir;
    private final Integer timeoutMs;
    private final Integer maxToolRounds;

    private ToolGateSettings(Builder builder) {
        this.enabled = builder.enabled;
        this.mcpUrl = builder.mcpUrl;
        this.caCertificate = builder.caCertificate;
        this.credentialsDir = builder.credentialsDir;
        this.timeoutMs = builder.timeoutMs;
        this.maxToolRounds = builder.maxToolRounds;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /** Gateway MCP endpoint, for example {@code https://localhost:18451/mcp}. */
    @JsonProperty("mcpUrl")
    public String getMcpUrl() {
        return mcpUrl;
    }

    /** PEM CA that signs the Gateway certificate. Unset uses the JVM trust store. */
    @JsonProperty("caCertificate")
    public String getCaCertificate() {
        return caCertificate;
    }

    /** Folder of {@code <agentName>.token} workload tokens. {@code OLO_TOOLGATE_TOKEN_<AGENT_NAME>} env vars win. */
    @JsonProperty("credentialsDir")
    public String getCredentialsDir() {
        return credentialsDir;
    }

    @JsonProperty("timeoutMs")
    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    /** Default tool rounds per agent turn when the AGENT node does not set {@code maxToolRounds}. */
    @JsonProperty("maxToolRounds")
    public Integer getMaxToolRounds() {
        return maxToolRounds;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public int resolveTimeoutMs() {
        return timeoutMs == null ? DEFAULT_TIMEOUT_MS : timeoutMs;
    }

    public int resolveMaxToolRounds() {
        return maxToolRounds == null ? DEFAULT_MAX_TOOL_ROUNDS : maxToolRounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ToolGateSettings that)) {
            return false;
        }
        return Objects.equals(enabled, that.enabled)
                && Objects.equals(mcpUrl, that.mcpUrl)
                && Objects.equals(caCertificate, that.caCertificate)
                && Objects.equals(credentialsDir, that.credentialsDir)
                && Objects.equals(timeoutMs, that.timeoutMs)
                && Objects.equals(maxToolRounds, that.maxToolRounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, mcpUrl, caCertificate, credentialsDir, timeoutMs, maxToolRounds);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Boolean enabled;
        private String mcpUrl;
        private String caCertificate;
        private String credentialsDir;
        private Integer timeoutMs;
        private Integer maxToolRounds;

        @JsonProperty("enabled")
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @JsonProperty("mcpUrl")
        public Builder mcpUrl(String mcpUrl) {
            this.mcpUrl = mcpUrl;
            return this;
        }

        @JsonProperty("caCertificate")
        public Builder caCertificate(String caCertificate) {
            this.caCertificate = caCertificate;
            return this;
        }

        @JsonProperty("credentialsDir")
        public Builder credentialsDir(String credentialsDir) {
            this.credentialsDir = credentialsDir;
            return this;
        }

        @JsonProperty("timeoutMs")
        public Builder timeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        @JsonProperty("maxToolRounds")
        public Builder maxToolRounds(Integer maxToolRounds) {
            this.maxToolRounds = maxToolRounds;
            return this;
        }

        public ToolGateSettings build() {
            return new ToolGateSettings(this);
        }
    }
}
