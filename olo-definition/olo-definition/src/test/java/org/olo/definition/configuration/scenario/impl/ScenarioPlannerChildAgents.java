/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario.impl;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

public final class ScenarioPlannerChildAgents {

    private ScenarioPlannerChildAgents() {
    }

    public static WorkflowDefinition childAgentPreset(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String... searchKeywords) {
        return WorkflowBuilder.create(name)
                .id(workflowId)
                .enabled(true)
                .isDefault(false)
                .role(name)
                .shortDescription(shortDescription)
                .emoji(emoji)
                .designer(StudioDesignerDefaults.studioAgentDesigner(emoji, searchKeywords))
                .queue(queue)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(CapabilityDefinition.builder()
                        .name(name)
                        .description(shortDescription)
                        .addTag(workflowId)
                        .addInput("input")
                        .addOutput("output")
                        .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                        .build())
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .presetPlannerContext(workflowId)
                .agentParameters(workflowId)
                .localAgentCanvasPipeline(workflowId)
                .metadata("description", shortDescription)
                .metadata("role", workflowId)
                .withStandardReturnVariable()
                .build();
    }
}
