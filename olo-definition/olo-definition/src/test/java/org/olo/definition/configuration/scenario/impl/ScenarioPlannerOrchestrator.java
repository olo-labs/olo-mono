/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario.impl;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.configuration.scenario.ScenarioConversationPluginSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioPlannerOrchestrator {

    private ScenarioPlannerOrchestrator() {
    }

    public static WorkflowBuilder orchestratorBuilder(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String promptTemplate,
            List<ScenarioAgentSpec> childAgents,
            List<ScenarioToolSpec> tools,
            ScenarioHumanActionSpec humanAction) {
        String plannerNodeId = ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        String humanNodeId = ScenarioHumanStepSupport.HUMAN_INPUT_NODE_ID;
        String conversationLoadNodeId = ScenarioConversationPluginSupport.CONVERSATION_LOAD_NODE_ID;
        String conversationStoreNodeId = ScenarioConversationPluginSupport.CONVERSATION_STORE_NODE_ID;
        HumanApprovalDefinition humanIntake = humanAction == null
                ? ScenarioHumanStepSupport.intakeForScenario(name, shortDescription)
                : ScenarioHumanStepSupport.intakeForScenario(
                        name, shortDescription, humanAction.actionLabel(), humanAction.requiredInputs());
        WorkflowBuilder builder = WorkflowBuilder.create(name)
                .id(workflowId)
                .enabled(true)
                .isDefault(true)
                .role(name)
                .shortDescription(shortDescription)
                .emoji(emoji)
                .designer(StudioDesignerDefaults.studioAgentDesigner(emoji, workflowId, "planner", "scenario"))
                .queue(queue)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .debuggable()
                .replayable()
                .capability(orchestratorCapability(name, shortDescription))
                .withMessageContract()
                .variable(ScenarioPlannerVariables.availableToolsVariable())
                .variable(ScenarioPlannerVariables.availableAgentsVariable())
                .variable(ScenarioPlannerVariables.agentResultsVariable())
                .variable(ScenarioPlannerVariables.toolCallSequenceVariable())
                .variable(ScenarioPlannerVariables.toolResultsVariable())
                .variable(ScenarioPlannerVariables.retryCountVariable())
                .variable(ScenarioPlannerVariables.validationErrorVariable())
                .variable(ScenarioPlannerVariables.conversationSummaryVariable())
                .variable(ScenarioPlannerVariables.conversationHistoryVariable())
                .defaultLocalModelInfrastructure()
                .agentParameters(workflowId)
                .agentPlannerMetadata()
                .agentDelegation()
                .startNodeWithMessageInput("start");
        ScenarioConversationPluginSupport.wireConversationPlugins(builder, conversationStoreNodeId);
        builder.humanNode(humanNodeId, "INPUT", humanIntake)
                .addNode(toolCallPlannerNode(plannerNodeId, promptTemplate, conversationStoreNodeId))
                .endNode("end")
                .connect("start", "out", conversationLoadNodeId, "in")
                .connect(conversationLoadNodeId, "out", humanNodeId, "in")
                .connect(humanNodeId, "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", conversationStoreNodeId, "in")
                .connect(conversationStoreNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(conversationLoadNodeId, 1)
                .nodeCanvasLayout(humanNodeId, 2)
                .nodeCanvasLayout(plannerNodeId, 3)
                .nodeCanvasLayout(conversationStoreNodeId, 4)
                .nodeCanvasLayout("end", 5)
                .metadata("description", shortDescription)
                .metadata("role", workflowId)
                .metadata(
                        ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION,
                        Map.of(
                                ToolCallPlannerSupport.METADATA_PLANNER_NODE_ID,
                                plannerNodeId,
                                ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_AGENTS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE,
                                ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID,
                                conversationStoreNodeId))
                .metadata(PlannerContextDefinition.METADATA_KEY, orchestratorPlannerContext(childAgents, tools))
                .withStandardReturnVariable();

        int agentColumn = 0;
        for (ScenarioAgentSpec childAgent : childAgents) {
            builder.childWorkflow(ChildWorkflowDefinition.builder()
                    .workflowId(childAgent.workflowId())
                    .workflowVersion("1.0.0")
                    .build());
            builder.availableAgent(childAgent.workflowId());
            builder.canvasChildAgentPluginNode(childAgent.workflowId(), childAgent.workflowId(), childAgent.label());
            builder.connect(childAgent.workflowId(), "agentPlug", plannerNodeId, "agentPlug");
            builder.nodeCanvasLayout(childAgent.workflowId(), agentColumn++);
        }

        int toolColumn = agentColumn;
        for (ScenarioToolSpec tool : tools) {
            builder.tool(scenarioTool(tool));
            builder.canvasToolNode(tool.nodeId());
            builder.putNodeConfiguration(tool.nodeId(), Map.of("toolId", tool.implementationId()));
            builder.connect(tool.nodeId(), "capabilities", plannerNodeId, "capabilities");
            builder.nodeCanvasLayout(tool.nodeId(), toolColumn++);
        }

        return builder;
    }

    public static ToolDefinition scenarioTool(ScenarioToolSpec spec) {
        return ToolDefinition.builder()
                .id(spec.nodeId())
                .capability(CapabilityDefinition.builder()
                        .name(spec.name())
                        .description(spec.description())
                        .addExample(spec.example())
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(spec.implementationId())
                        .build())
                .build();
    }

    private static CapabilityDefinition orchestratorCapability(String name, String description) {
        return CapabilityDefinition.builder()
                .name(name)
                .description(description)
                .addTag("agent")
                .addTag("scenario")
                .addInput("input")
                .addOutput("output")
                .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .build();
    }

    private static Map<String, Object> orchestratorPlannerContext(
            List<ScenarioAgentSpec> childAgents, List<ScenarioToolSpec> tools) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(
                PlannerContextDefinition.SELECTED_VARIABLES,
                List.of(
                        WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                        ScenarioConversationPluginSupport.CONVERSATION_SUMMARY_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE));
        context.put(PlannerContextDefinition.SELECTED_TOOLS, tools.stream().map(ScenarioToolSpec::implementationId).toList());
        context.put(
                PlannerContextDefinition.SELECTED_AGENTS,
                childAgents.stream().map(ScenarioAgentSpec::workflowId).toList());
        context.put(PlannerContextDefinition.INJECT_CAPABILITIES, false);
        context.put(PlannerContextDefinition.INJECT_AGENTS, true);
        return Map.copyOf(context);
    }

    private static NodeDefinition toolCallPlannerNode(String nodeId, String promptTemplate, String continueNodeId) {
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
                .putConfiguration("promptTemplate", promptTemplate)
                .build();
    }
}
