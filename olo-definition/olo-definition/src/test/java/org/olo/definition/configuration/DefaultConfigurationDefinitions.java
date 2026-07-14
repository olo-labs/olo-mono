/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.configuration.agenttool.AgentToolExecutionDefinitions;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.parameter.ParameterUiDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.preset.WorkflowConversationPluginSupport;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

/**
 * Programmatic builders for preset workflows under {@code olo-configuration/default/}.
 */
final class DefaultConfigurationDefinitions {

    static final String OLO_QUEUE_1 = "oloQueue1";
    static final String OLO_QUEUE_2 = "oloQueue2";

    private DefaultConfigurationDefinitions() {
    }

    private static WorkflowDefinition build(WorkflowBuilder builder) {
        return builder.withStandardReturnVariable().build();
    }

    private static CapabilityDefinition agentCapability(String name, String description, String tag) {
        return CapabilityDefinition.builder()
                .name(name)
                .description(description)
                .addTag(tag)
                .addInput("input")
                .addOutput("output")
                .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .build();
    }

    private static DesignerDefinition agentDesigner(String emoji, String... searchKeywords) {
        return StudioDesignerDefaults.studioAgentDesigner(emoji, searchKeywords);
    }

    private static WorkflowDefinition agentPreset(
            String id, String queue, String name, String shortDescription, String emoji, String... searchKeywords) {
        return build(WorkflowBuilder.create(name)
                .id(id)
                .enabled(true)
                .isDefault(true)
                .role(name)
                .shortDescription(shortDescription)
                .emoji(emoji)
                .designer(agentDesigner(emoji, searchKeywords))
                .queue(queue)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(agentCapability(name, shortDescription, id))
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .presetPlannerContext(id)
                .agentParameters(id)
                .localAgentCanvasPipeline(id)
                .metadata("description", shortDescription)
                .metadata("role", id));
    }

    static WorkflowDefinition agent() {
        return AgentToolExecutionDefinitions.agent();
    }

    static WorkflowDefinition architect() {
        return build(WorkflowBuilder.from(agentPreset(
                        "architect",
                        OLO_QUEUE_1,
                        "Architect",
                        "System design and architecture guidance",
                        "🏗️",
                        "architect"))
                .enabled(false));
    }

    static WorkflowDefinition ask() {
        return agentPreset("ask", OLO_QUEUE_1, "Ask", "Direct questions and clear answers", "❓", "ask");
    }

    static WorkflowDefinition debug() {
        String description = "Verbose output for troubleshooting";
        return build(WorkflowBuilder.create("Debug")
                .id("debug")
                .enabled(true)
                .isDefault(true)
                .role("Debug")
                .shortDescription(description)
                .emoji("🐛")
                .designer(agentDesigner("🐛", "debug"))
                .queue(OLO_QUEUE_2)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .debuggable()
                .replayable()
                .capability(agentCapability("Debug", description, "debug"))
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .presetPlannerContext("debug")
                .agentParameters("debug")
                .localAgentCanvasPipeline("debug")
                .metadata("description", description)
                .metadata("role", "debug"));
    }

    static WorkflowDefinition detailed() {
        return agentPreset("detailed", OLO_QUEUE_1, "Detailed", "Thorough, in-depth explanations", "📖", "detailed");
    }

    static WorkflowDefinition fast() {
        return agentPreset("fast", OLO_QUEUE_2, "Fast", "Quick, concise responses", "⚡", "fast");
    }

    static WorkflowDefinition planner() {
        return agentPreset(
                "planner",
                OLO_QUEUE_1,
                "Planner",
                "Structured plans and task breakdowns",
                "📋",
                "planner");
    }


    static WorkflowDefinition ragChat() {
        String description = "Answers using retrieved knowledge from an indexed capability source";
        String ragQueryNodeId = "rag-query";
        return build(WorkflowBuilder.create("RAG Chat")
                .id("rag-chat")
                .enabled(true)
                .isDefault(false)
                .role("RAG Chat")
                .shortDescription(description)
                .emoji("\uD83D\uDD0E")
                .designer(agentDesigner("\uD83D\uDD0E", "rag", "chat", "knowledge"))
                .queue(OLO_QUEUE_2)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(agentCapability("RAG Chat", "Retrieve indexed chunks then answer the user question", "rag"))
                .withMessageContract()
                .variable(VariableDefinition.builder()
                        .name("conversationSummary")
                        .type("string")
                        .scope(VariableScope.LOCAL)
                        .build())
                .variable(VariableDefinition.builder()
                        .name("ragContext")
                        .type("string")
                        .scope(VariableScope.LOCAL)
                        .metadata(java.util.Map.of("role", "rag-context"))
                        .build())
                .parameter("systemPrompt", WorkflowParameterDefinition.builder()
                        .type("string")
                        .label("System Prompt")
                        .defaultValue("You answer using the retrieved knowledge context when provided.\n\nRetrieved context:\n{ragContext}\n\nQuestion:\n{message}\n\nPrevious conversation summary:\n{conversationSummary}\n\nAnswer concisely and cite the retrieved context when relevant.")
                        .ui(ParameterUiDefinition.builder()
                                .widget("TEXTAREA")
                                .group("Model Settings")
                                .order(0)
                                .build())
                        .build())
                .parameter("temperature", WorkflowParameterDefinition.builder()
                        .type("number")
                        .defaultValue(0.2)
                        .ui(ParameterUiDefinition.builder()
                                .widget("SLIDER")
                                .group("Model Settings")
                                .order(1)
                                .build())
                        .build())
                .parameter("maxIterations", WorkflowParameterDefinition.builder()
                        .type("integer")
                        .defaultValue(6)
                        .ui(ParameterUiDefinition.builder()
                                .widget("NUMBER")
                                .group("Model Settings")
                                .order(2)
                                .build())
                        .build())
                .defaultLocalModelInfrastructure()
                .metadata("plannerContext", java.util.Map.of(
                        "selectedVariables", java.util.List.of("message", "conversationSummary", "ragContext"),
                        "injectCapabilities", false))
                .metadata("description", description)
                .metadata("role", "rag-chat")
                .startNodeWithMessageInput("start")
                .toolNode(WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID)
                .putNodeConfiguration(
                        WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID,
                        java.util.Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_LOAD_TOOL_ID))
                .canvasToolNode(ragQueryNodeId, "RAG Query")
                .putNodeConfiguration(ragQueryNodeId, java.util.Map.of(
                        "toolId", "olo-core:rag-query",
                        "extensionRef", "pgvector-store",
                        "vectorTable", "documents",
                        "driver", "qdrant",
                        "connectionRef", "http://localhost:46333",
                        "collection", "documents",
                        "vectorSize", 384,
                        "distance", "Cosine",
                        "topK", 5,
                        "scoreThreshold", 0.25))
                .addNode(NodeDefinition.builder()
                        .id("agent")
                        .type(NodeType.AGENT.name())
                        .workflow(WorkflowReferenceDefinition.builder()
                                .workflowId("rag-chat")
                                .version("1.0.0")
                                .build())
                        .executionKind(ExecutionKind.ACTIVITY)
                        .executionModel(ExecutionModel.INLINE)
                        .addPort(WorkflowBuilder.messagePort("in", org.olo.definition.port.PortDirection.INPUT))
                        .addPort(WorkflowBuilder.messagePort("out", org.olo.definition.port.PortDirection.OUTPUT))
                        .build())
                .toolNode(WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID)
                .putNodeConfiguration(
                        WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID,
                        java.util.Map.of("toolId", WorkflowConversationPluginSupport.CONVERSATION_STORE_TOOL_ID))
                .endNode("end")
                .connect("start", "out", WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID, "in")
                .connect(WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID, "out", ragQueryNodeId, "in")
                .connect(ragQueryNodeId, "out", "agent", "in")
                .connect("agent", "out", WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID, "in")
                .connect(WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID, "out", "end", "in")
                .extension(org.olo.definition.extension.ExtensionDefinition.builder()
                        .id("pgvector-store")
                        .type("VECTOR_STORE")
                        .configuration(java.util.Map.of(
                                "driver", "qdrant",
                                "connectionRef", "http://localhost:46333",
                                "table", "documents",
                                "collection", "documents",
                                "vectorSize", 384,
                                "distance", "Cosine"))
                        .build())
                .tool(ToolDefinition.builder()
                        .id("rag-query")
                        .capability(CapabilityDefinition.builder()
                                .name("RAG Query")
                                .description("Retrieves indexed chunks from Qdrant")
                                .addExample("Retrieve selected documents for capabilitySource my-knowledge-base")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("olo-core:rag-query")
                                .build())
                        .build()));
    }

    static WorkflowDefinition reviewer() {
        return agentPreset("reviewer", OLO_QUEUE_2, "Reviewer", "Review code and content critically", "🔍", "reviewer");
    }

    static WorkflowDefinition strict() {
        return agentPreset("strict", OLO_QUEUE_1, "Strict", "Precise, rule-following responses", "📏", "strict");
    }

    static WorkflowDefinition summary() {
        return agentPreset("summary", OLO_QUEUE_2, "Summary", "Brief summaries and key points", "📝", "summary");
    }

    static WorkflowDefinition teacher() {
        return agentPreset("teacher", OLO_QUEUE_1, "Teacher", "Learn concepts step by step", "🎓", "teacher");
    }

    /** Minimal echo task-queue preset ({@code workflow.json} on disk, id {@code minimal-echo}). */
    static WorkflowDefinition workflow() {
        String description = "Smallest valid " + OloProductTerminology.WORKFLOW + ": passes input through to output.";
        return build(WorkflowBuilder.create("Minimal Echo")
                .id("minimal-echo")
                .enabled(true)
                .isDefault(true)
                .role("Echo")
                .shortDescription("Minimal passthrough workflow")
                .emoji("💬")
                .designer(StudioDesignerDefaults.studioAgentDesigner("💬", "minimal", "echo"))
                .queue(OLO_QUEUE_2)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(CapabilityDefinition.builder()
                        .name("Minimal Echo")
                        .description(description)
                        .addInput("input")
                        .addOutput("output")
                        .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                        .build())
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .presetPlannerContext("minimal-echo")
                .agentParameters("minimal-echo")
                .localAgentCanvasPipeline("minimal-echo")
                .metadata("description", description));
    }
}
