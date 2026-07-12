/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.agenttool.impl;

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
import static org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions.WORKFLOW_ID;

public final class AgentToolExecutionWorkflowBuilder {

    private AgentToolExecutionWorkflowBuilder() {
    }

    public static WorkflowDefinition build() {
        String description = "Autonomous tool-using agent with strict JSON tool-call planning";
        String plannerNodeId = ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        return WorkflowBuilder.create("Agent")
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
                .startNodeWithMessageInput("start")
                .canvasToolNode(CALCULATOR_NODE_ID)
                .putNodeConfiguration(CALCULATOR_NODE_ID, Map.of("toolId", CALCULATOR_TOOL_ID))
                .canvasToolNode(CPU_USAGE_NODE_ID)
                .putNodeConfiguration(CPU_USAGE_NODE_ID, Map.of("toolId", CPU_USAGE_TOOL_ID))
                .addNode(AgentToolPlannerSupport.toolCallPlannerNode(plannerNodeId))
                .endNode("end")
                .connect("start", "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", "end", "in")
                .connect(CALCULATOR_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .connect(CPU_USAGE_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(plannerNodeId, 1)
                .nodeCanvasLayout("end", 2)
                .nodeCanvasLayout(CALCULATOR_NODE_ID, 3)
                .nodeCanvasLayout(CPU_USAGE_NODE_ID, 4)
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
                                "end"))
                .metadata(PlannerContextDefinition.METADATA_KEY, AgentToolPlannerSupport.agentPlannerContext())
                .withStandardReturnVariable()
                .build();
    }
}
