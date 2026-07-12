/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.agenttool.impl;

import org.olo.definition.configuration.agenttool.impl.AgentToolPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioConversationPluginSupport;
import org.olo.definition.configuration.scenario.impl.ScenarioHumanStepSupport;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Map;

import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CALCULATOR_NODE_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CALCULATOR_TOOL_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CPU_USAGE_NODE_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.CPU_USAGE_TOOL_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.RESTART_CONTAINER_NODE_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.RESTART_CONTAINER_TOOL_ID;
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.WORKFLOW_ID;

public final class AgentToolExecutionWorkflowBuilder {

    private AgentToolExecutionWorkflowBuilder() {
    }

    public static WorkflowDefinition build() {
        String description = "Autonomous tool-using agent with strict JSON tool-call planning";
        String plannerNodeId = ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        String humanNodeId = ScenarioHumanStepSupport.HUMAN_INPUT_NODE_ID;
        String conversationLoadNodeId = ScenarioConversationPluginSupport.CONVERSATION_LOAD_NODE_ID;
        String conversationStoreNodeId = ScenarioConversationPluginSupport.CONVERSATION_STORE_NODE_ID;
        WorkflowBuilder builder = WorkflowBuilder.create("Agent")
                .id(WORKFLOW_ID)
                .enabled(true)
                .isDefault(true)
                .role("Agent")
                .shortDescription(description)
                .emoji("🤖")
                .designer(StudioDesignerDefaults.studioAgentDesigner("🤖", "planning", "task", "agent"))
                .queue("oloQueue2")
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .debuggable()
                .replayable()
                .capability(AgentToolPlannerSupport.agentCapability(description))
                .withMessageContract()
                .variable(AgentToolComponentDefinitions.availableToolsVariable())
                .variable(AgentToolComponentDefinitions.toolCallSequenceVariable())
                .variable(AgentToolComponentDefinitions.toolResultsVariable())
                .variable(AgentToolComponentDefinitions.retryCountVariable())
                .variable(AgentToolComponentDefinitions.validationErrorVariable())
                .variable(AgentToolComponentDefinitions.conversationSummaryVariable())
                .variable(AgentToolComponentDefinitions.conversationHistoryVariable())
                .defaultLocalModelInfrastructure()
                .agentParameters()
                .agentPlannerMetadata()
                .agentAvailableAgents()
                .agentDelegation()
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("planner")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("fast")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("detailed")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("reviewer")
                        .workflowVersion("1.0.0")
                        .build())
                .tool(AgentToolComponentDefinitions.calculatorTool())
                .tool(AgentToolComponentDefinitions.cpuUsageTool())
                .tool(AgentToolComponentDefinitions.restartContainerTool())
                .startNodeWithMessageInput("start");
        ScenarioConversationPluginSupport.wireConversationPlugins(builder, conversationStoreNodeId);
        return builder.humanNode(humanNodeId, "INPUT", ScenarioHumanStepSupport.agentToolIntake())
                .canvasToolNode(CALCULATOR_NODE_ID)
                .putNodeConfiguration(CALCULATOR_NODE_ID, Map.of("toolId", CALCULATOR_TOOL_ID))
                .canvasToolNode(CPU_USAGE_NODE_ID)
                .putNodeConfiguration(CPU_USAGE_NODE_ID, Map.of("toolId", CPU_USAGE_TOOL_ID))
                .canvasToolNode(RESTART_CONTAINER_NODE_ID)
                .putNodeConfiguration(RESTART_CONTAINER_NODE_ID, Map.of("toolId", RESTART_CONTAINER_TOOL_ID))
                .addNode(AgentToolPlannerSupport.toolCallPlannerNode(plannerNodeId, conversationStoreNodeId))
                .endNode("end")
                .connect("start", "out", conversationLoadNodeId, "in")
                .connect(conversationLoadNodeId, "out", humanNodeId, "in")
                .connect(humanNodeId, "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", conversationStoreNodeId, "in")
                .connect(conversationStoreNodeId, "out", "end", "in")
                .connect(CALCULATOR_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .connect(CPU_USAGE_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .connect(RESTART_CONTAINER_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(conversationLoadNodeId, 1)
                .nodeCanvasLayout(humanNodeId, 2)
                .nodeCanvasLayout(plannerNodeId, 3)
                .nodeCanvasLayout(conversationStoreNodeId, 4)
                .nodeCanvasLayout("end", 5)
                .nodeCanvasLayout(CALCULATOR_NODE_ID, 6)
                .nodeCanvasLayout(CPU_USAGE_NODE_ID, 7)
                .nodeCanvasLayout(RESTART_CONTAINER_NODE_ID, 8)
                .metadata("description", description)
                .metadata("role", WORKFLOW_ID)
                .metadata(
                        ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION,
                        Map.of(
                                ToolCallPlannerSupport.METADATA_PLANNER_NODE_ID,
                                plannerNodeId,
                                ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID,
                                conversationStoreNodeId))
                .metadata(PlannerContextDefinition.METADATA_KEY, AgentToolPlannerSupport.agentPlannerContext())
                .withStandardReturnVariable()
                .build();
    }
}
