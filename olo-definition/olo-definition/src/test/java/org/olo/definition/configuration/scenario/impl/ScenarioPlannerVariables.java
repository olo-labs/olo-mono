/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario.impl;

import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;

import java.util.Map;

final class ScenarioPlannerVariables {

    private ScenarioPlannerVariables() {
    }

    static VariableDefinition availableAgentsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE)
                .type("string")
                .description("JSON array of child agents connected on the canvas agentPlug port")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "available-agents"))
                .build();
    }

    static VariableDefinition availableToolsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .type("string")
                .description("JSON array of tools connected on the canvas and registered in tools[]")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "available-tools"))
                .build();
    }

    static VariableDefinition agentResultsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE)
                .type("string")
                .description("Aggregated JSON results from child agent workflows delegated by the planner")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "agent-results"))
                .build();
    }

    static VariableDefinition toolCallSequenceVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .type("string")
                .description("Model-produced strict JSON tool call sequence")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "tool-call-sequence"))
                .build();
    }

    static VariableDefinition toolResultsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE)
                .type("string")
                .description("Aggregated JSON results from executed tool nodes for synthesis")
                .scope(VariableScope.LOCAL)
                .build();
    }

    static VariableDefinition retryCountVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE)
                .type("number")
                .description("Invalid JSON retry counter for the tool-call planner")
                .scope(VariableScope.LOCAL)
                .build();
    }

    static VariableDefinition validationErrorVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE)
                .type("string")
                .description("Last validation error from toolCallSequenceJson, injected into planner retries")
                .scope(VariableScope.LOCAL)
                .build();
    }
}
