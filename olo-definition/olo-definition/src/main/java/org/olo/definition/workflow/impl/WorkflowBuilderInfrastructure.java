/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.preset.WorkflowConversationPluginSupport;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Model providers, message contract, return variable, and planner context helpers for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderInfrastructure {

    public static final String RETURN_VARIABLE_METADATA_KEY = "returnVariable";
    public static final String RETURN_VARIABLE_NAME = "ReturnValue";
    public static final String RETURN_VARIABLE_ROLE = "return";

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;
    private final WorkflowBuilderNodes nodes;
    private final WorkflowBuilderCore core;

    public WorkflowBuilderInfrastructure(
            WorkflowBuilderState state,
            WorkflowBuilder owner,
            WorkflowBuilderNodes nodes,
            WorkflowBuilderCore core) {
        this.state = state;
        this.owner = owner;
        this.nodes = nodes;
        this.core = core;
    }

    // --- model infrastructure ---

    public WorkflowBuilder modelProvider(ModelProviderDefinition provider) {
        Objects.requireNonNull(provider, "provider is required");
        state.modelProviders.add(provider);
        return owner;
    }

    public WorkflowBuilder modelRouting(ModelRoutingDefinition routing) {
        Objects.requireNonNull(routing, "routing is required");
        state.modelRouting.add(routing);
        return owner;
    }

    public WorkflowBuilder defaultLocalModelInfrastructure() {
        if (state.modelProviders.isEmpty()) {
            modelProvider(WorkflowPresetInfrastructure.defaultLocalModelProvider());
        }
        if (state.modelRouting.isEmpty()) {
            modelRouting(WorkflowPresetInfrastructure.defaultModelRouting());
        }
        return owner;
    }

    // --- message contract ---

    public WorkflowBuilder withMessageInput() {
        if (!state.inputs.containsKey(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)) {
            nodes.input(
                    WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                    WorkflowInputDefinition.builder()
                            .schema("string")
                            .required(true)
                            .build());
        }
        return owner;
    }

    public WorkflowBuilder withMessageVariable() {
        boolean hasMessage = state.variables.stream()
                .anyMatch(variable -> WorkflowPresetInfrastructure.MESSAGE_VARIABLE.equals(variable.getName()));
        if (!hasMessage) {
            nodes.variable(WorkflowPresetInfrastructure.messageVariable());
        }
        return owner;
    }

    public WorkflowBuilder withMessageContract() {
        return withMessageInput().withMessageVariable();
    }

    public WorkflowBuilder withConversationVariables() {
        addVariableIfAbsent(WorkflowConversationPluginSupport.conversationSummaryVariable());
        addVariableIfAbsent(WorkflowConversationPluginSupport.conversationHistoryVariable());
        return owner;
    }

    // --- planner context presets ---

    public WorkflowBuilder presetPlannerContext(String presetId) {
        return core.metadata(
                org.olo.definition.planner.PlannerContextDefinition.METADATA_KEY,
                org.olo.definition.planner.PlannerContextDefinition.presetDefaults(presetId));
    }

    /**
     * Declares the standard workflow return variable and {@code metadata.returnVariable = ReturnValue}.
     */
    public WorkflowBuilder withStandardReturnVariable() {
        state.metadata.putIfAbsent(RETURN_VARIABLE_METADATA_KEY, RETURN_VARIABLE_NAME);
        boolean hasReturnVariable = state.variables.stream()
                .anyMatch(variable -> RETURN_VARIABLE_NAME.equals(variable.getName()));
        if (!hasReturnVariable) {
            nodes.variable(VariableDefinition.builder()
                    .name(RETURN_VARIABLE_NAME)
                    .type("string")
                    .description("Workflow return message returned to the caller")
                    .required(false)
                    .scope(org.olo.definition.variable.VariableScope.LOCAL)
                    .defaultValue(null)
                    .metadata(Map.of("role", RETURN_VARIABLE_ROLE))
                    .build());
        }
        return owner;
    }

    private void addVariableIfAbsent(VariableDefinition variable) {
        boolean exists = state.variables.stream()
                .anyMatch(existing -> variable.getName().equals(existing.getName()));
        if (!exists) {
            nodes.variable(variable);
        }
    }
}
