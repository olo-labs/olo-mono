/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.planner.AgentAvailableAgents;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.planner.WorkflowPlannerMetadata;
import org.olo.definition.runtime.AgentDelegationPolicy;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.RuntimeDelegationDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.Objects;

/**
 * Runtime contract, capability, delegation, and agent preset helpers for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderRuntime {

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;
    private final WorkflowBuilderNodes nodes;
    private final WorkflowBuilderCore core;

    public WorkflowBuilderRuntime(
            WorkflowBuilderState state,
            WorkflowBuilder owner,
            WorkflowBuilderNodes nodes,
            WorkflowBuilderCore core) {
        this.state = state;
        this.owner = owner;
        this.nodes = nodes;
        this.core = core;
    }

    // --- capability and runtime contract ---

    /**
     * Planner-readable contract for this workflow (required for valid workflows).
     */
    public WorkflowBuilder capability(CapabilityDefinition capability) {
        Objects.requireNonNull(capability, "capability is required");
        state.delegate.capability(capability);
        return owner;
    }

    public WorkflowBuilder runtime(WorkflowRuntimeDefinition runtime) {
        state.delegate.runtime(runtime);
        state.workflowRuntimeBuilder = null;
        return owner;
    }

    /**
     * Sets {@code runtime.executionModel} (catalog-aligned: {@code INLINE}, {@code ACTIVITY},
     * {@code CHILD_WORKFLOW}, {@code EXTERNAL}).
     */
    public WorkflowBuilder executionModel(ExecutionModel executionModel) {
        state.workflowRuntimeBuilder().executionModel(executionModel);
        return owner;
    }

    public WorkflowBuilder defaultTimeout(String defaultTimeout) {
        state.workflowRuntimeBuilder().defaultTimeout(defaultTimeout);
        return owner;
    }

    /** {@code CHILD_WORKFLOW} agent runtime with debug, replay, timeout, and a 10-minute default. */
    public WorkflowBuilder agentWorkflowRuntime() {
        return executionModel(ExecutionModel.CHILD_WORKFLOW)
                .debuggable()
                .replayable()
                .addRuntimeCapability(RuntimeCapability.TIMEOUT)
                .defaultTimeout("PT10M");
    }

    public WorkflowBuilder runtimeBinding(RuntimeBindingDefinition runtimeBinding) {
        state.delegate.runtimeBinding(runtimeBinding);
        return owner;
    }

    public WorkflowBuilder debuggable() {
        state.workflowRuntimeBuilder().debuggable(true);
        return owner;
    }

    public WorkflowBuilder replayable() {
        state.workflowRuntimeBuilder().replayable(true);
        return owner;
    }

    public WorkflowBuilder addRuntimeCapability(RuntimeCapability capability) {
        state.workflowRuntimeBuilder().addCapability(capability);
        return owner;
    }

    // --- agent parameters ---

    /** Agent tuning parameters with catalog defaults ({@code systemPrompt} = {@code {message}}). */
    public WorkflowBuilder baselineAgentParameters() {
        AgentWorkflowParameters.defaults().forEach(nodes::parameter);
        return owner;
    }

    /** Agent tuning parameters for the {@code agent} preset. */
    public WorkflowBuilder agentParameters() {
        return agentParameters("agent");
    }

    /** Agent tuning parameters with a preset-specific system prompt default. */
    public WorkflowBuilder agentParameters(String presetId) {
        AgentWorkflowParameters.forPreset(presetId).forEach(nodes::parameter);
        return owner;
    }

    // --- planner routing and available agents ---

    /** Planner routing metadata ({@link WorkflowPlannerMetadata}). */
    public WorkflowBuilder agentPlannerMetadata() {
        WorkflowPlannerMetadata.agentDefaults().forEach(core::metadata);
        return owner;
    }

    /** Default planner context ({@link org.olo.definition.planner.PlannerContextDefinition}). */
    public WorkflowBuilder agentPlannerContext() {
        return core.metadata(
                org.olo.definition.planner.PlannerContextDefinition.METADATA_KEY,
                org.olo.definition.planner.PlannerContextDefinition.agentDefaults());
    }

    public WorkflowBuilder availableAgent(String agentWorkflowId) {
        return availableAgent(AgentReferenceDefinition.of(agentWorkflowId));
    }

    public WorkflowBuilder availableAgent(AgentReferenceDefinition agentReference) {
        state.availableAgents.add(agentReference);
        return owner;
    }

    /** Default {@code availableAgents} for the agent preset ({@link AgentAvailableAgents}). */
    public WorkflowBuilder agentAvailableAgents() {
        AgentAvailableAgents.agentPresetDefaults().forEach(this::availableAgent);
        return owner;
    }

    // --- delegation ---

    public WorkflowBuilder delegation(RuntimeDelegationDefinition delegation) {
        state.workflowRuntimeBuilder().delegation(delegation);
        return owner;
    }

    /** Delegation guardrails for the agent preset ({@link AgentDelegationPolicy}). */
    public WorkflowBuilder agentDelegation() {
        return delegation(AgentDelegationPolicy.agentPresetDefaults());
    }
}
