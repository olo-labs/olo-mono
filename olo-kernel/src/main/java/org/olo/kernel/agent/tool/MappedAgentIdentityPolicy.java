/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * {@link AgentIdentityPolicy} backed by explicit (tenant, workflow) → agents mappings.
 *
 * <ul>
 *   <li>The workflow and agent must match exactly. The tenant matches exactly, or {@code "*"} matches any tenant.</li>
 *   <li>Reserved principals (the platform service principal) are refused even if a mapping lists them.</li>
 * </ul>
 */
public final class MappedAgentIdentityPolicy implements AgentIdentityPolicy {

    public static final String ANY_TENANT = "*";

    /** One mapping entry. */
    public record Mapping(String tenant, String workflow, Collection<String> agents) {
        public Mapping {
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(workflow, "workflow");
            agents = List.copyOf(agents);
        }
    }

    private final List<Mapping> mappings;
    private final Set<String> reserved;

    public MappedAgentIdentityPolicy(List<Mapping> mappings, Collection<String> reservedPrincipals) {
        this.mappings = List.copyOf(mappings);
        this.reserved = new HashSet<>();
        for (String principal : reservedPrincipals) {
            reserved.add(principal.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public void authorize(String tenantId, String workflowId, String agentName) {
        if (agentName == null || reserved.contains(agentName.toLowerCase(Locale.ROOT))) {
            throw new AgentIdentityDeniedException(tenantId, workflowId, agentName, "it is a reserved platform principal");
        }
        String tenant = tenantId == null ? "" : tenantId;
        for (Mapping mapping : mappings) {
            boolean tenantMatches = ANY_TENANT.equals(mapping.tenant()) || mapping.tenant().equals(tenant);
            if (tenantMatches && mapping.workflow().equals(workflowId) && mapping.agents().contains(agentName)) {
                return;
            }
        }
        throw new AgentIdentityDeniedException(tenantId, workflowId, agentName, "no mapping allows it");
    }
}
