/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.agenttool.impl;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;

import java.util.Map;

import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CALCULATOR_NODE_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CALCULATOR_TOOL_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CPU_USAGE_NODE_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CPU_USAGE_TOOL_ID;

final class AgentToolComponentDefinitions {

    private AgentToolComponentDefinitions() {
    }

    static ToolDefinition calculatorTool() {
        return ToolDefinition.builder()
                .id(CALCULATOR_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("Calculator")
                        .description("Basic arithmetic on two numbers")
                        .addExample("Compute order totals")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CALCULATOR_TOOL_ID)
                        .build())
                .build();
    }

    static ToolDefinition cpuUsageTool() {
        return ToolDefinition.builder()
                .id(CPU_USAGE_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("CPU Usage")
                        .description(
                                "Reads CPU usage metrics from CSV files in a configured folder filtered by time range")
                        .addExample("Check CPU spike during payment gateway outage")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CPU_USAGE_TOOL_ID)
                        .build())
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
