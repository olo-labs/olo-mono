/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.agenttool.impl;

import org.olo.definition.configuration.scenario.ScenarioConversationPluginSupport;
import org.olo.definition.OloProductTerminology;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.planner.AgentAvailableAgents;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CALCULATOR_TOOL_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CPU_USAGE_TOOL_ID;

final class AgentToolPlannerSupport {

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            "You are an "
                    + OloProductTerminology.PRODUCT
                    + """
                     tool-call planner.
            """
                    + ScenarioConversationPluginSupport.conversationContextPromptBlock()
                    + """

                    User request:
            {message}

            Available tools (strict allow-list — use only these toolId values):
            {availableToolsJson}

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Do not wrap the JSON in ```json``` blocks.
            3. Schema:
            {
              "toolCalls": [
                { "toolId": "olo-core:calculator", "arguments": { } }
              ],
              "directResponse": null
            }
            4. If no tools are needed, set "toolCalls": [] and put the final answer in "directResponse".
            5. If tools are needed, set "directResponse": null and list toolCalls in execution order.
            6. Each toolId MUST appear in availableToolsJson.
            7. Include arguments when a tool needs structured input (for example ISO-8601 startTime/endTime for observability tools).
            8. Final human-approved action — when the operator approved a container restart at human-input, call
               olo-core:restart-container with containerId and namespace. Include confirmationId and logPath in directResponse.

            If a previous attempt failed validation, fix it:
            {toolCallSequenceJsonValidationError}

            Respond with JSON only.""";

    private AgentToolPlannerSupport() {
    }

    static CapabilityDefinition agentCapability(String description) {
        return CapabilityDefinition.builder()
                .name("Agent")
                .description(description)
                .addTag("agent")
                .addInput("input")
                .addOutput("output")
                .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .build();
    }

    static Map<String, Object> agentPlannerContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(
                PlannerContextDefinition.SELECTED_VARIABLES,
                List.of(
                        WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                        ScenarioConversationPluginSupport.CONVERSATION_SUMMARY_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE));
        context.put(PlannerContextDefinition.SELECTED_TOOLS, List.of(CALCULATOR_TOOL_ID, CPU_USAGE_TOOL_ID));
        context.put(PlannerContextDefinition.SELECTED_AGENTS, AgentAvailableAgents.agentPresetDefaults().stream()
                .map(ref -> ref.getId())
                .toList());
        context.put(PlannerContextDefinition.INJECT_CAPABILITIES, false);
        return Map.copyOf(context);
    }

    static NodeDefinition toolCallPlannerNode(String nodeId, String continueNodeId) {
        return NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.AGENT.name())
                .label("Agent")
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .addPort(WorkflowBuilder.messagePort("in", PortDirection.INPUT))
                .addPort(WorkflowBuilder.capabilitiesPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.agentPlugPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT))
                .putConfiguration(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true)
                .putConfiguration(
                        ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .putConfiguration(
                        ToolCallPlannerSupport.CONFIG_MAX_INVALID_JSON_RETRIES,
                        ToolCallPlannerSupport.DEFAULT_MAX_INVALID_JSON_RETRIES)
                .putConfiguration(ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID, continueNodeId)
                .putConfiguration("promptTemplate", JSON_ONLY_PROMPT_TEMPLATE)
                .build();
    }
}
