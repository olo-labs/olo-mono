/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.runtime;

import org.olo.spi.context.ExecutionContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default mutable {@link ExecutionContext} backed by an in-memory variable map.
 */
public final class DefaultExecutionContext implements ExecutionContext {

    private final String workflowId;
    private final String runId;
    private final String queue;
    private final String correlationId;
    private String nodeId;
    private final Map<String, Object> variables = new LinkedHashMap<>();

    public DefaultExecutionContext(String workflowId, String runId, String queue, String correlationId) {
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId");
        this.runId = Objects.requireNonNull(runId, "runId");
        this.queue = Objects.requireNonNull(queue, "queue");
        this.correlationId = correlationId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public String getRunId() {
        return runId;
    }

    @Override
    public String getQueue() {
        return queue;
    }

    @Override
    public Optional<String> getNodeId() {
        return Optional.ofNullable(nodeId).filter(s -> !s.isBlank());
    }

    @Override
    public Optional<String> getCorrelationId() {
        return Optional.ofNullable(correlationId).filter(s -> !s.isBlank());
    }

    @Override
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    @Override
    public Object getVariable(String name) {
        return variables.get(name);
    }

    @Override
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    @Override
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}
