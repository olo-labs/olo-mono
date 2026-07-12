/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.node.NodeRouterDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.parallel.JoinDefinition;
import org.olo.definition.parallel.JoinStrategy;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.List;
import java.util.Map;

import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.buildSample;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.messageBranchPort;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.multiInputModelNode;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.multiInputOutputNode;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.nodeWithDefaultPorts;

/** Routing, HITL, and parallel fan-out orchestration samples. */
public final class SampleOrchestrationWorkflows {

    private SampleOrchestrationWorkflows() {
    }

    public static WorkflowDefinition conditionBranch() {
        return buildSample(WorkflowBuilder.create("Conditional Branch")
                .id("condition-branch")
                .version("1.0.0")
                .capability(CapabilityDefinition.builder()
                        .name("Conditional Branch")
                        .description("Routes user intent to support or sales agent workflows.")
                        .addInput("intent")
                        .addOutput("response")
                        .addTag("routing")
                        .build())
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("router", NodeType.CONDITION)
                        .version("1.0.0")
                        .addPort(messageBranchPort("true"))
                        .addPort(messageBranchPort("false"))
                        .putConfiguration("expression", "input.intent == 'support'")
                        .addRouter(NodeRouterDefinition.builder()
                                .id("to-support")
                                .name("support")
                                .targetPort("true")
                                .targetNodeId("support-agent")
                                .match(Map.of("intent", "support"))
                                .build())
                        .addRouter(NodeRouterDefinition.builder()
                                .id("to-sales")
                                .name("sales")
                                .targetPort("false")
                                .targetNodeId("sales-agent")
                                .match(Map.of("intent", "sales"))
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("support-agent", NodeType.AGENT)
                        .version("1.0.0")
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("support-agent")
                                .version("1.0.0")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("sales-agent", NodeType.AGENT)
                        .version("1.0.0")
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("sales-agent")
                                .version("1.0.0")
                                .build())
                        .build())
                .addNode(multiInputOutputNode("output"))
                .connect("input", "router")
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("router")
                        .sourcePortId("true")
                        .targetNodeId("support-agent")
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("router")
                        .sourcePortId("false")
                        .targetNodeId("sales-agent")
                        .build())
                .connect("support-agent", "output")
                .connect("sales-agent", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .build())
                .metadata("description", "Demonstrates port-aware edges on a CONDITION node."));
    }

    public static WorkflowDefinition humanApprovalTrade() {
        return buildSample(WorkflowBuilder.create("Trade Approval")
                .id("human-approval-trade")
                .version("1.0.0")
                .capability(CapabilityDefinition.builder()
                        .name("Trade Approval")
                        .description("AI trade recommendation with human approval before execution.")
                        .addInput("tradeRequest")
                        .addOutput("executionResult")
                        .addTag("finance")
                        .addTag("hitl")
                        .build())
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("recommendation", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "openai-default")
                        .build())
                .humanNode(
                        "trade-approval",
                        HumanApprovalDefinition.builder()
                                .title("Approve trade execution?")
                                .description("Review the AI recommendation before executing the trade.")
                                .approvers(List.of("trading-desk"))
                                .timeoutSeconds(3600L)
                                .requireCommentOnReject(true)
                                .build())
                .addNode(nodeWithDefaultPorts("execute-trade", NodeType.TOOL)
                        .putConfiguration("toolRef", "trade-executor")
                        .build())
                .outputNode("output")
                .connect("input", "recommendation")
                .connect("recommendation", "trade-approval")
                .connect("trade-approval", "execute-trade")
                .connect("execute-trade", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("trade-executor")
                        .type("TOOL")
                        .putConfiguration("implementation", "com.example.tools.TradeExecutor")
                        .build())
                .metadata(
                        "description",
                        "AI recommendation → human approval → trade execution (enterprise HITL)."));
    }

    public static WorkflowDefinition parallelAgentFanOut() {
        return buildSample(WorkflowBuilder.create("Parallel Agents")
                .id("parallel-agent-fan-out")
                .version("1.0.0")
                .capability(CapabilityDefinition.builder()
                        .name("Parallel Agent Fan-Out")
                        .description("Runs research, news, and risk agents in parallel then synthesizes.")
                        .addInput("query")
                        .addOutput("synthesis")
                        .addTag("multi-agent")
                        .addTag("parallel")
                        .build())
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("fan-out", NodeType.PARALLEL)
                        .join(JoinDefinition.builder().strategy(JoinStrategy.ALL).build())
                        .build())
                .addNode(nodeWithDefaultPorts("research-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("research-agent")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("news-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("news-agent")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("risk-agent", NodeType.AGENT)
                        .executionKind(ExecutionKind.SUBWORKFLOW)
                        .executionModel(ExecutionModel.CHILD_WORKFLOW)
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("risk-agent")
                                .build())
                        .build())
                .addNode(multiInputModelNode("synthesis", "CHAT"))
                .outputNode("output")
                .connect("input", "fan-out")
                .connect("fan-out", "research-agent")
                .connect("fan-out", "news-agent")
                .connect("fan-out", "risk-agent")
                .connect("research-agent", "synthesis")
                .connect("news-agent", "synthesis")
                .connect("risk-agent", "synthesis")
                .connect("synthesis", "output")
                .metadata("description", "PARALLEL fan-out with join ALL, then synthesis MODEL."));
    }
}
