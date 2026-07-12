/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.context;

import java.util.Map;
import java.util.Optional;

/**
 * Mutable runtime view of a single workflow execution: identity, routing, and workflow variables.
 * <p>
 * Populated by the runtime engine before invoking nodes, tools, or hooks.
 */
public interface ExecutionContext {

    String getWorkflowId();

    String getRunId();

    String getQueue();

    Optional<String> getNodeId();

    Optional<String> getCorrelationId();

    boolean hasVariable(String name);

    Object getVariable(String name);

    void setVariable(String name, Object value);

    Map<String, Object> getVariables();
}
