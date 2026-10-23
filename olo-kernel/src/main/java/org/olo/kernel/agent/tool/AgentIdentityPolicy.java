/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

/**
 * Decides whether a run may act as a given ToolGate agent identity. The decision comes from operator-controlled
 * configuration, never from the workflow definition, so a workflow author can't select an identity they weren't given.
 */
public interface AgentIdentityPolicy {

    /**
     * @throws AgentIdentityDeniedException when {@code agentName} isn't authorized for this tenant and workflow
     */
    void authorize(String tenantId, String workflowId, String agentName);

    /** Refuses every identity. The safe default when no mappings are configured. */
    static AgentIdentityPolicy denyAll() {
        return (tenantId, workflowId, agentName) -> {
            throw new AgentIdentityDeniedException(tenantId, workflowId, agentName, "no agent mappings are configured");
        };
    }
}
