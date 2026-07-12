/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.buildSample;

/** Standalone agent workflow samples with planner-readable capability contracts. */
public final class SampleAgentWorkflows {

    private SampleAgentWorkflows() {
    }

    public static WorkflowDefinition technicalAnalysisAgent() {
        return buildSample(WorkflowBuilder.create("Technical Analysis Agent")
                .id("technical-analysis")
                .version("1.0.0")
                .capability(CapabilityDefinition.builder()
                        .name("Technical Analysis Agent")
                        .description("Analyze indicators and trends")
                        .addInput("symbol")
                        .addOutput("analysis")
                        .build())
                .agent(AgentDefinition.builder()
                        .id("technical-analysis")
                        .capability(CapabilityDefinition.builder()
                                .name("Technical Analysis Agent")
                                .description("Analyze indicators and trends")
                                .build())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("technical-analysis-v1")
                                .version("1.0.0")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("default-agent-runner")
                                .build())
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .systemPrompt("You are a technical analysis expert.")
                        .build())
                .inputNode("input")
                .outputNode("output")
                .connect("input", "output"));
    }

    public static WorkflowDefinition researchAgent() {
        return buildSample(WorkflowBuilder.create("Research Agent")
                .id("research-agent")
                .role("agent")
                .shortDescription("Web and document research with citations")
                .longDescription(
                        "Performs web, news and document research, summarizes findings and produces citations.")
                .isChildWorkflow(true)
                .version("2.1.0")
                .capability(CapabilityDefinition.builder()
                        .id("research-agent")
                        .name("Research Agent")
                        .description(
                                "Performs web and document research, summarizes findings and provides citations.")
                        .addTag("research")
                        .addTag("news")
                        .addTag("documents")
                        .addInput("query")
                        .addOutput("summary")
                        .addOutput("citations")
                        .addExample("Research Nvidia earnings")
                        .addExample("Compare AI startups")
                        .cost(0.15)
                        .latency(45_000.0)
                        .confidence(0.85)
                        .addToolRequirement("web-search")
                        .addRequiredContext("user_profile")
                        .build())
                .inputNode("input")
                .modelNode("research", "CHAT")
                .outputNode("output")
                .connect("input", "research")
                .connect("research", "output")
                .metadata("description", "Agent workflow with full planner-readable capability contract."));
    }
}
