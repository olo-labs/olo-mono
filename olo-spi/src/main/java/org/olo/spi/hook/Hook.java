/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.hook;

import org.olo.spi.context.ExecutionContext;

/**
 * Cross-cutting hook implementation (tracing, metrics, audit, etc.).
 */
public interface Hook {

    /**
     * Registry id matching {@code HookActionDefinition.implementationId} in the workflow graph.
     */
    String implementationId();

    /**
     * Runs the hook for the given phase and request.
     */
    HookResult run(HookRequest request, ExecutionContext context);
}
