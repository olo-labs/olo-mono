/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.kernel.agent.tool.AgentToolAccess;

import java.util.Objects;

/**
 * Holds the workflow definition registry for kernel activities running inside the Temporal worker.
 */
public final class KernelRuntimeHolder {

    private static volatile WorkflowDefinitionRegistry registry;
    private static volatile AgentToolAccess toolAccess = AgentToolAccess.disabled();

    private KernelRuntimeHolder() {
    }

    public static void setRegistry(WorkflowDefinitionRegistry workflowRegistry) {
        registry = Objects.requireNonNull(workflowRegistry, "workflowRegistry");
    }

    public static WorkflowDefinitionRegistry registry() {
        WorkflowDefinitionRegistry current = registry;
        if (current == null) {
            throw new IllegalStateException("workflow definition registry not initialized");
        }
        return current;
    }

    /** Installs worker-wide ToolGate access for LLM agents. */
    public static void setToolAccess(AgentToolAccess access) {
        toolAccess = Objects.requireNonNull(access, "access");
    }

    /** ToolGate access for LLM agents; {@link AgentToolAccess#disabled()} until the worker installs it. */
    public static AgentToolAccess toolAccess() {
        return toolAccess;
    }

    public static void reset() {
        registry = null;
        toolAccess = AgentToolAccess.disabled();
    }
}
