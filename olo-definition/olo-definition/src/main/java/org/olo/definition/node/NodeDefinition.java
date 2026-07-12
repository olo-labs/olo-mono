/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.error.OnFailureDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.hook.NodeHooksDefinition;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.parallel.JoinDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single node in a workflow graph.
 * <p>
 * Structured as: {@code id}/{@code type} (identity), {@code capability} (what it is),
 * {@code ports} (typed connection contracts referenced by edges), {@code reads}/{@code writes}
 * (workflow state access), {@code execution} (how it runs), {@code configuration} (integration settings).
 */
@JsonDeserialize(builder = NodeDefinitionBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeDefinition {

    private final String id;
    private final String type;
    private final String label;
    private final CapabilityDefinition capability;
    private final List<PortDefinition> ports;
    private final NodeExecutionDefinition execution;
    private final List<String> reads;
    private final List<String> writes;
    private final Map<String, Object> configuration;
    private final NodeHooksDefinition hooks;

    NodeDefinition(NodeDefinitionBuilder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.label = builder.label;
        this.capability = builder.capability;
        this.ports = builder.ports == null ? List.of() : List.copyOf(builder.ports);
        this.execution = builder.execution;
        this.reads = builder.reads == null ? List.of() : List.copyOf(builder.reads);
        this.writes = builder.writes == null ? List.of() : List.copyOf(builder.writes);
        this.configuration = builder.configuration == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.configuration));
        this.hooks = builder.hooks;
    }

    public static NodeDefinitionBuilder builder() {
        return new NodeDefinitionBuilder();
    }

    public String getId() { return id; }
    public String getType() { return type; }

    /** Canvas / studio display name for this node instance. */
    public String getLabel() { return label; }

    public CapabilityDefinition getCapability() { return capability; }
    public List<PortDefinition> getPorts() { return ports; }
    public NodeExecutionDefinition getExecution() { return execution; }

    /** Declarative workflow state reads (e.g. {@code state.symbol}). */
    public List<String> getReads() { return reads; }

    /** Declarative workflow state writes (e.g. {@code state.analysis}). */
    public List<String> getWrites() { return writes; }

    public Map<String, Object> getConfiguration() { return configuration; }

    /** Optional per-node hook bindings (implementation ids must be registered on workflow-level hooks). */
    public NodeHooksDefinition getHooks() { return hooks; }

    @JsonIgnore public ExecutionKind getExecutionKind() {
        return execution == null ? null : execution.getExecutionKind();
    }

    @JsonIgnore public ExecutionModel getExecutionModel() {
        return execution == null ? null : execution.getExecutionModel();
    }

    @JsonIgnore public WorkflowReferenceDefinition getWorkflow() {
        return execution == null ? null : execution.getWorkflow();
    }

    @JsonIgnore public JoinDefinition getJoin() {
        return execution == null ? null : execution.getJoin();
    }

    @JsonIgnore public String getSubtype() {
        return execution == null ? null : execution.getSubtype();
    }

    @JsonIgnore public String getVersion() {
        return execution == null ? null : execution.getVersion();
    }

    @JsonIgnore public List<NodeRouterDefinition> getRouters() {
        return execution == null ? List.of() : execution.getRouters();
    }

    @JsonIgnore public OnFailureDefinition getOnFailure() {
        return execution == null ? null : execution.getOnFailure();
    }

    @JsonIgnore public HumanApprovalDefinition getApproval() {
        return execution == null ? null : execution.getApproval();
    }

    @JsonIgnore public RuntimeBindingDefinition getRuntimeBinding() {
        return execution == null ? null : execution.getRuntimeBinding();
    }

    @Override
    public boolean equals(Object o) {
        return NodeDefinitionEquality.equals(this, o);
    }

    @Override
    public int hashCode() {
        return NodeDefinitionEquality.hashCode(this);
    }

    @Override
    public String toString() {
        return NodeDefinitionEquality.toString(this);
    }
}
