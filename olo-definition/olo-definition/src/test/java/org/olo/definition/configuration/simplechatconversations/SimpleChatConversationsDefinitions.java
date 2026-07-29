/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.simplechatconversations;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.preset.WorkflowConversationPluginSupport;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;

/** Programmatic builders for the {@code simple-chat-conversations} pipeline collection. */
public final class SimpleChatConversationsDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String END_USER_REPLY_ID = "end-user-reply-chat";
    public static final String ARCHITECT_ID = "architect-chat";
    public static final String TRAVELER_ID = "traveler-chat";
    public static final String LITERATURE_ID = "literature-chat";
    public static final String TEACHER_ID = "teacher-chat";
    public static final String REVIEWER_ID = "reviewer-chat";

    private static final List<SimpleChatSpec> SPECS = List.of(
            new SimpleChatSpec(
                    END_USER_REPLY_ID,
                    "End User Reply Chat",
                    "Friendly end-user support replies with conversation memory",
                    "chat",
                    "support",
                    prompt("""
                            You are a friendly end-user support assistant. Reply in plain language, acknowledge the user's immediate need, and keep the conversation moving.
                            """)),
            new SimpleChatSpec(
                    ARCHITECT_ID,
                    "Architect Chat",
                    "Architecture-focused design conversation with chat history",
                    "architecture",
                    "design",
                    prompt("""
                            You are a software architect. Discuss trade-offs, boundaries, interfaces, operational risks, and pragmatic next steps.
                            """)),
            new SimpleChatSpec(
                    TRAVELER_ID,
                    "Traveler Chat",
                    "Travel planning conversation with preferences remembered",
                    "travel",
                    "itinerary",
                    prompt("""
                            You are a travel conversation specialist. Remember preferences, ask only necessary follow-up questions, and suggest practical itinerary options.
                            """)),
            new SimpleChatSpec(
                    LITERATURE_ID,
                    "Literature Chat",
                    "Literature and reading conversation with prior context",
                    "literature",
                    "reading",
                    prompt("""
                            You are a literature conversation partner. Discuss books, papers, themes, evidence, and interpretation with careful nuance.
                            """)),
            new SimpleChatSpec(
                    TEACHER_ID,
                    "Teacher Chat",
                    "Teaching conversation that builds from prior turns",
                    "teacher",
                    "learning",
                    prompt("""
                            You are a patient teacher. Build understanding step by step, adapt to the learner's prior turns, and check comprehension gently.
                            """)),
            new SimpleChatSpec(
                    REVIEWER_ID,
                    "Reviewer Chat",
                    "Review-focused conversation with history-aware critique",
                    "reviewer",
                    "critique",
                    prompt("""
                            You are a reviewer. Give balanced, specific critique, call out risks and gaps, and suggest concrete improvements.
                            """)));

    private SimpleChatConversationsDefinitions() {
    }

    public static List<String> workflowIds() {
        return SPECS.stream().map(SimpleChatSpec::id).toList();
    }

    public static WorkflowDefinition endUserReplyChat() {
        return workflow(END_USER_REPLY_ID);
    }

    public static WorkflowDefinition architectChat() {
        return workflow(ARCHITECT_ID);
    }

    public static WorkflowDefinition travelerChat() {
        return workflow(TRAVELER_ID);
    }

    public static WorkflowDefinition literatureChat() {
        return workflow(LITERATURE_ID);
    }

    public static WorkflowDefinition teacherChat() {
        return workflow(TEACHER_ID);
    }

    public static WorkflowDefinition reviewerChat() {
        return workflow(REVIEWER_ID);
    }

    public static WorkflowDefinition workflow(String workflowId) {
        SimpleChatSpec spec = SPECS.stream()
                .filter(candidate -> candidate.id().equals(workflowId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown simple chat workflow: " + workflowId));
        WorkflowDefinition workflow = WorkflowBuilder.create(spec.label())
                .id(spec.id())
                .enabled(true)
                .isDefault(false)
                .role(spec.label())
                .shortDescription(spec.description())
                .emoji("\uD83D\uDCAC")
                .designer(StudioDesignerDefaults.studioAgentDesigner("\uD83D\uDCAC", spec.primaryKeyword(), spec.secondaryKeyword(), "chat"))
                .queue(QUEUE)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(CapabilityDefinition.builder()
                        .name(spec.label())
                        .description(spec.description())
                        .addTag("chat")
                        .addTag(spec.primaryKeyword())
                        .addInput("input")
                        .addOutput("output")
                        .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                        .addRequiredContext(WorkflowConversationPluginSupport.CONVERSATION_SUMMARY_VARIABLE)
                        .build())
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .baselineAgentParameters()
                .parameter(AgentWorkflowParameters.SYSTEM_PROMPT, systemPromptParameter(spec.prompt()))
                .presetPlannerContext(spec.id())
                .localAgentCanvasPipeline(spec.id())
                .withStandardReturnVariable()
                .metadata("description", spec.description())
                .metadata("role", spec.id())
                .metadata("collection", "simple-chat-conversations")
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    private static WorkflowParameterDefinition systemPromptParameter(String prompt) {
        WorkflowParameterDefinition base = AgentWorkflowParameters.defaults().get(AgentWorkflowParameters.SYSTEM_PROMPT);
        return WorkflowParameterDefinition.builder()
                .type(base.getType())
                .label(base.getLabel())
                .description(base.getDescription())
                .defaultValue(prompt)
                .required(base.getRequired())
                .validation(base.getValidation())
                .visibleWhen(base.getVisibleWhen())
                .ui(base.getUi())
                .build();
    }

    private static String prompt(String roleInstruction) {
        return roleInstruction.strip()
                + """

                Current user message:
                {message}

                Reference-only prior conversation context:
                {conversationSummary}

                Use prior context only for continuity. Do not quote, summarize, mention, or answer it unless the current user message explicitly asks about prior conversation.
                Answer as the assigned role while staying focused on the current user message.
                """;
    }

    private record SimpleChatSpec(
            String id,
            String label,
            String description,
            String primaryKeyword,
            String secondaryKeyword,
            String prompt) {
    }
}
