/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MappedAgentIdentityPolicyTest {

    private final AgentIdentityPolicy policy = new MappedAgentIdentityPolicy(List.of(
            new MappedAgentIdentityPolicy.Mapping("tenant-a", "incident-recovery", List.of("triage-agent", "remediation-agent")),
            new MappedAgentIdentityPolicy.Mapping("*", "triage-agent", List.of("triage-agent"))),
            List.of("olo-platform-service"));

    @Test
    void allowsExactlyTheMappedTenantWorkflowAndAgent() {
        assertThatCode(() -> policy.authorize("tenant-a", "incident-recovery", "remediation-agent")).doesNotThrowAnyException();
        assertThatCode(() -> policy.authorize("tenant-z", "triage-agent", "triage-agent")).doesNotThrowAnyException();
    }

    @Test
    void refusesAnotherTenantWorkflowOrAgent() {
        assertThatThrownBy(() -> policy.authorize("tenant-b", "incident-recovery", "triage-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class);
        assertThatThrownBy(() -> policy.authorize("tenant-a", "other-workflow", "triage-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class);
        assertThatThrownBy(() -> policy.authorize("tenant-a", "incident-recovery", "incident-report-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class);
    }

    @Test
    void missingTenantMatchesOnlyAnyTenantMappings() {
        assertThatCode(() -> policy.authorize(null, "triage-agent", "triage-agent")).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.authorize(null, "incident-recovery", "triage-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class);
    }

    @Test
    void reservedPrincipalIsRefusedEvenIfListed() {
        AgentIdentityPolicy listsIt = new MappedAgentIdentityPolicy(
                List.of(new MappedAgentIdentityPolicy.Mapping("*", "w", List.of("OLO-Platform-Service"))),
                List.of("olo-platform-service"));

        assertThatThrownBy(() -> listsIt.authorize("t", "w", "OLO-Platform-Service"))
                .isInstanceOf(AgentIdentityDeniedException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void denyAllRefusesEverything() {
        assertThatThrownBy(() -> AgentIdentityPolicy.denyAll().authorize("t", "w", "triage-agent"))
                .isInstanceOf(AgentIdentityDeniedException.class)
                .hasMessageContaining("no agent mappings");
    }
}
