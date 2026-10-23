/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import org.olo.kernel.exception.KernelException;

/**
 * A run tried to act as a ToolGate agent identity that operators haven't mapped to its tenant and workflow.
 */
public final class AgentIdentityDeniedException extends KernelException {

    public AgentIdentityDeniedException(String tenantId, String workflowId, String agentName, String reason) {
        super("agent identity '" + agentName + "' is not authorized for workflow '" + workflowId
                + "' in tenant '" + (tenantId == null || tenantId.isBlank() ? "(none)" : tenantId) + "': " + reason
                + ". Add it to toolgate.agentMappings in the worker configuration.");
    }
}
