/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.support;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

public final class ToolCallSubgraphMergerTestWorkflows {

    private ToolCallSubgraphMergerTestWorkflows() {
    }

    public static WorkflowDefinition orchestratorWithChildAgents() {
        return WorkflowBuilder.from(baseWorkflow())
                .childWorkflow(org.olo.definition.workflow.ChildWorkflowDefinition.builder()
                        .workflowId("literature-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(org.olo.definition.workflow.ChildWorkflowDefinition.builder()
                        .workflowId("synthesis-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("literature-agent")
                        .type(NodeType.AGENT.name())
                        .label("Literature Agent")
                        .putConfiguration("delegateAgentId", "literature-agent")
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("synthesis-agent")
                        .type(NodeType.AGENT.name())
                        .label("Synthesis Agent")
                        .putConfiguration("delegateAgentId", "synthesis-agent")
                        .build())
                .connect("literature-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .connect("synthesis-agent", "agentPlug", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "agentPlug")
                .build();
    }

    public static WorkflowDefinition baseWorkflowWithTools() {
        return WorkflowBuilder.from(baseWorkflow())
                .tool(ToolDefinition.builder()
                        .id("calculator")
                        .capability(CapabilityDefinition.builder()
                                .name("Calculator")
                                .description("Calculator tool")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("olo-core:calculator")
                                .build())
                        .build())
                .tool(ToolDefinition.builder()
                        .id("cpu-usage")
                        .capability(CapabilityDefinition.builder()
                                .name("CPU Usage")
                                .description("CPU usage tool")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("olo-core:cpu-usage")
                                .build())
                        .build())
                .build();
    }

    public static WorkflowDefinition baseWorkflow() {
        return WorkflowBuilder.create("Agent")
                .id("agent")
                .queue("agent")
                .startNode("start")
                .addNode(plannerNode())
                .endNode("end")
                .connect("start", "out", ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "in")
                .connect(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID, "out", "end", "in")
                .build();
    }

    private static NodeDefinition plannerNode() {
        return NodeDefinition.builder()
                .id(ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID)
                .type(NodeType.AGENT.name())
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .putConfiguration(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true)
                .build();
    }
}
