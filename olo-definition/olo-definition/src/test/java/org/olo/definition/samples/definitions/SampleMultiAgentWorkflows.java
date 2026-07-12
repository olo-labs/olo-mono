/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.buildSample;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.nodeWithDefaultPorts;

/** Multi-agent orchestration composing planner and specialized child agent workflows. */
public final class SampleMultiAgentWorkflows {

    private SampleMultiAgentWorkflows() {
    }

    public static WorkflowDefinition multiAgentOrchestration() {
        return buildSample(WorkflowBuilder.create("Multi-Agent Orchestration")
                .id("multi-agent-orchestration")
                .version("1.0.0")
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("research-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("risk-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("execution-agent")
                        .workflowVersion("1.0.0")
                        .build())
                .capability(CapabilityDefinition.builder()
                        .name("Multi-Agent Orchestration")
                        .description("Composes planner and specialized agent workflows in sequence.")
                        .addInput("goal")
                        .addOutput("result")
                        .addTag("multi-agent")
                        .build())
                .agent(AgentDefinition.builder()
                        .id("research-agent")
                        .capability(CapabilityDefinition.builder()
                                .name("Research Agent")
                                .description("Research topics and summarize findings")
                                .addExample("Research sector trends")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("research-agent")
                                .version("1.0.0")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("default-agent-runner")
                                .build())
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .systemPrompt("You research topics and summarize findings.")
                        .build())
                .agent(AgentDefinition.builder()
                        .id("risk-agent")
                        .capability(CapabilityDefinition.builder()
                                .name("Risk Agent")
                                .description("Evaluate investment risk")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("risk-agent")
                                .version("1.0.0")
                                .build())
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .build())
                .agent(AgentDefinition.builder()
                        .id("execution-agent")
                        .capability(CapabilityDefinition.builder()
                                .name("Execution Agent")
                                .description("Execute approved investment actions")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("execution-agent")
                                .version("1.0.0")
                                .build())
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .build())
                .input("symbol", WorkflowInputDefinition.builder()
                        .schema("String")
                        .required(true)
                        .build())
                .stateField("marketData", StateFieldDefinition.builder().schema("MarketData").build())
                .stateField("news", StateFieldDefinition.builder().schema("News[]").build())
                .stateField("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .inputNode("input")
                .modelNode("planner", "PLANNER")
                .addNode(nodeWithDefaultPorts("research-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .addRead("state.symbol")
                        .addRead("state.news")
                        .addWrite("state.analysis")
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("research-agent")
                                .version("1.0.0")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("risk-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .addRead("state.symbol")
                        .addRead("state.marketData")
                        .addRead("state.analysis")
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("risk-agent")
                                .version("1.0.0")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("execution-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .addRead("state.analysis")
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("execution-agent")
                                .version("1.0.0")
                                .build())
                        .build())
                .outputNode("output")
                .connect("input", "planner")
                .connect("planner", "research-agent")
                .connect("research-agent", "risk-agent")
                .connect("risk-agent", "execution-agent")
                .connect("execution-agent", "output")
                .metadata("description", "Multi-agent = workflow composing agent workflows (agent handoff)."));
    }
}
