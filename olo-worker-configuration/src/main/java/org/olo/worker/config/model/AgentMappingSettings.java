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
 * Server-authorized agent identities for one tenant and workflow. Workflow authors can't edit this: it lives in the
 * operator-controlled worker configuration, and an AGENT node's {@code agentName} must appear here.
 *
 * <p>{@code tenant: "*"} matches any tenant, for single-tenant installations. The workflow and agent names are always
 * exact.
 */
@JsonDeserialize(builder = AgentMappingSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AgentMappingSettings {

    public static final String ANY_TENANT = "*";

    private final String tenant;
    private final String workflow;
    private final List<String> agents;

    private AgentMappingSettings(Builder builder) {
        this.tenant = builder.tenant;
        this.workflow = builder.workflow;
        this.agents = builder.agents == null ? List.of() : List.copyOf(builder.agents);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("tenant")
    public String getTenant() {
        return tenant;
    }

    @JsonProperty("workflow")
    public String getWorkflow() {
        return workflow;
    }

    @JsonProperty("agents")
    public List<String> getAgents() {
        return agents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentMappingSettings that)) {
            return false;
        }
        return Objects.equals(tenant, that.tenant)
                && Objects.equals(workflow, that.workflow)
                && Objects.equals(agents, that.agents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, workflow, agents);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String tenant;
        private String workflow;
        private List<String> agents;

        @JsonProperty("tenant")
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        @JsonProperty("workflow")
        public Builder workflow(String workflow) {
            this.workflow = workflow;
            return this;
        }

        @JsonProperty("agents")
        public Builder agents(List<String> agents) {
            this.agents = agents;
            return this;
        }

        public AgentMappingSettings build() {
            return new AgentMappingSettings(this);
        }
    }
}
