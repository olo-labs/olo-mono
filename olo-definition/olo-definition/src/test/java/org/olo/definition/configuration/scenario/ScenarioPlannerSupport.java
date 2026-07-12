/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario;

import org.olo.definition.configuration.scenario.impl.ScenarioPlannerChildAgents;
import org.olo.definition.configuration.scenario.impl.ScenarioPlannerOrchestrator;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Shared builders for scenario planner collections (orchestrator + child agent presets). */
public final class ScenarioPlannerSupport {

    private ScenarioPlannerSupport() {
    }

    public static WorkflowDefinition childAgentPreset(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String... searchKeywords) {
        return ScenarioPlannerChildAgents.childAgentPreset(
                workflowId, queue, name, shortDescription, emoji, searchKeywords);
    }

    public static WorkflowBuilder orchestratorBuilder(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String promptTemplate,
            List<ScenarioAgentSpec> childAgents,
            List<ScenarioToolSpec> tools) {
        return ScenarioPlannerOrchestrator.orchestratorBuilder(
                workflowId, queue, name, shortDescription, emoji, promptTemplate, childAgents, tools);
    }

    public static ToolDefinition scenarioTool(ScenarioToolSpec spec) {
        return ScenarioPlannerOrchestrator.scenarioTool(spec);
    }

    public record ScenarioAgentSpec(String workflowId, String label) {
    }

    public record ScenarioToolSpec(
            String nodeId,
            String implementationId,
            String name,
            String description,
            String example) {
    }
}
