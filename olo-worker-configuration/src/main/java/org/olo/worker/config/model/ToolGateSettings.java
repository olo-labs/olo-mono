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

import java.util.List;
import java.util.Objects;

/**
 * OLO ToolGate Gateway settings. When enabled, every LLM agent asks ToolGate for the tools its agent
 * name may use and calls them through the Gateway.
 *
 * <p>An AGENT node's {@code agentName} is used only if {@code agentMappings} authorizes it for the run's tenant and
 * workflow. Names in {@code reservedPrincipals} (the platform service principal) can never be mapped.
 *
 * <p>Relative {@code caCertificate} and {@code credentialsDir} paths resolve against the configuration file's folder.
 */
@JsonDeserialize(builder = ToolGateSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolGateSettings {

    public static final int DEFAULT_TIMEOUT_MS = 60_000;
    public static final int DEFAULT_MAX_TOOL_ROUNDS = 8;
    public static final List<String> DEFAULT_RESERVED_PRINCIPALS = List.of("olo-platform-service");

    private final Boolean enabled;
    private final String mcpUrl;
    private final String caCertificate;
    private final String credentialsDir;
    private final Integer timeoutMs;
    private final Integer maxToolRounds;
    private final List<AgentMappingSettings> agentMappings;
    private final List<String> reservedPrincipals;

    private ToolGateSettings(Builder builder) {
        this.enabled = builder.enabled;
        this.mcpUrl = builder.mcpUrl;
        this.caCertificate = builder.caCertificate;
        this.credentialsDir = builder.credentialsDir;
        this.timeoutMs = builder.timeoutMs;
        this.maxToolRounds = builder.maxToolRounds;
        this.agentMappings = builder.agentMappings == null ? List.of() : List.copyOf(builder.agentMappings);
        this.reservedPrincipals = builder.reservedPrincipals == null ? null : List.copyOf(builder.reservedPrincipals);
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

    /** Server-authorized agent identities per tenant and workflow. Empty means no agent may use ToolGate. */
    @JsonProperty("agentMappings")
    public List<AgentMappingSettings> getAgentMappings() {
        return agentMappings;
    }

    /** Principals that can never be mapped to a workflow agent. Unset means {@link #DEFAULT_RESERVED_PRINCIPALS}. */
    @JsonProperty("reservedPrincipals")
    public List<String> getReservedPrincipals() {
        return reservedPrincipals;
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

    public List<String> resolveReservedPrincipals() {
        return reservedPrincipals == null ? DEFAULT_RESERVED_PRINCIPALS : reservedPrincipals;
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
                && Objects.equals(maxToolRounds, that.maxToolRounds)
                && Objects.equals(agentMappings, that.agentMappings)
                && Objects.equals(reservedPrincipals, that.reservedPrincipals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, mcpUrl, caCertificate, credentialsDir, timeoutMs, maxToolRounds,
                agentMappings, reservedPrincipals);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Boolean enabled;
        private String mcpUrl;
        private String caCertificate;
        private String credentialsDir;
        private Integer timeoutMs;
        private Integer maxToolRounds;
        private List<AgentMappingSettings> agentMappings;
        private List<String> reservedPrincipals;

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

        @JsonProperty("agentMappings")
        public Builder agentMappings(List<AgentMappingSettings> agentMappings) {
            this.agentMappings = agentMappings;
            return this;
        }

        @JsonProperty("reservedPrincipals")
        public Builder reservedPrincipals(List<String> reservedPrincipals) {
            this.reservedPrincipals = reservedPrincipals;
            return this;
        }

        public ToolGateSettings build() {
            return new ToolGateSettings(this);
        }
    }
}
