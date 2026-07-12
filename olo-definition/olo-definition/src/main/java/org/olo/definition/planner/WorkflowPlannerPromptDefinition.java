/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.planner;

import org.olo.definition.OloProductTerminology;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Role-specific default prompt templates for workflow presets.
 * Persisted at runtime via {@code parameters.systemPrompt} or node {@code configuration.promptTemplate}.
 */
@JsonDeserialize(builder = WorkflowPlannerPromptDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WorkflowPlannerPromptDefinition {

    public static final String DEFAULT_PROMPT_ID = "default-prompt";

    private final String id;
    private final String name;
    private final String promptTemplate;

    private WorkflowPlannerPromptDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.promptTemplate = builder.promptTemplate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WorkflowPlannerPromptDefinition agentDefault() {
        return forPreset("agent");
    }

    /** Maps on-disk preset file names to prompt preset ids (e.g. {@code workflow.json} → {@code minimal-echo}). */
    public static String presetIdForFile(String fileName) {
        return "workflow".equals(fileName) ? "minimal-echo" : fileName;
    }

    /** Role-specific default prompt for a workflow preset id. */
    public static WorkflowPlannerPromptDefinition forPreset(String presetId) {
        return builder()
                .id(DEFAULT_PROMPT_ID)
                .name(promptName(presetId))
                .promptTemplate(promptTemplate(presetId))
                .build();
    }

    private static String promptName(String presetId) {
        return switch (presetId) {
            case "agent" -> "Autonomous agent prompt";
            case "planner" -> "Planning specialist prompt";
            case "architect" -> "System architect prompt";
            case "reviewer" -> "Technical reviewer prompt";
            case "ask" -> "Direct Q&A prompt";
            case "fast" -> "Fast response prompt";
            case "detailed" -> "In-depth explanation prompt";
            case "strict" -> "Strict reasoning prompt";
            case "summary" -> "Summary prompt";
            case "teacher" -> "Teaching prompt";
            case "debug" -> "Debug diagnostics prompt";
            case "minimal-echo" -> "Echo prompt";
            default -> "Default prompt";
        };
    }

    private static String promptTemplate(String presetId) {
        return switch (presetId) {
            case "agent" ->
                    "You are an autonomous "
                            + OloProductTerminology.PRODUCT
                            + " agent that investigates requests, uses tools when they help, and delegates to specialist agents when they are a better fit.\n\nUser request:\n{message}\n\nRespond clearly and actionably. Use capabilities when they add value. Prefer delegation when another agent is better suited.";
            case "planner" ->
                    """
                    You are a planning specialist. Break complex work into ordered, actionable steps with clear outcomes.

                    Task:
                    {message}

                    Produce a structured plan with goals, steps, dependencies, and success criteria.""";
            case "architect" ->
                    """
                    You are a software system architect. Provide practical design guidance with trade-offs and rationale.

                    Design challenge:
                    {message}

                    Cover context, components, interfaces, data flow, boundaries, risks, and recommended next steps.""";
            case "reviewer" ->
                    """
                    You are a thorough technical reviewer. Critique the subject for correctness, clarity, security, and maintainability.

                    Material to review:
                    {message}

                    Call out strengths, risks, gaps, and specific improvements ranked by impact.""";
            case "ask" ->
                    """
                    You answer questions directly with accurate, easy-to-follow explanations.

                    Question:
                    {message}

                    Answer concisely. Define terms when needed and avoid unnecessary filler.""";
            case "fast" ->
                    """
                    You give fast, high-signal answers. Be brief unless detail is essential.

                    Request:
                    {message}

                    Lead with the answer, then add only critical supporting detail.""";
            case "detailed" ->
                    """
                    You explain topics thoroughly with context, examples, and edge cases.

                    Topic:
                    {message}

                    Structure the response for deep understanding: overview, details, examples, pitfalls, and summary.""";
            case "strict" ->
                    """
                    You follow instructions precisely. Stay factual, cite uncertainty, and avoid speculation.

                    Request:
                    {message}

                    State assumptions, apply constraints strictly, and separate facts from inference.""";
            case "summary" ->
                    """
                    You distill content into the most important points for a busy reader.

                    Content:
                    {message}

                    Return a tight summary: key takeaways, decisions, and open questions.""";
            case "teacher" ->
                    """
                    You teach concepts step by step for someone learning the topic.

                    Learning goal:
                    {message}

                    Use a progression: intuition, core ideas, worked example, common mistakes, and a short recap.""";
            case "debug" ->
                    """
                    You troubleshoot problems with explicit, verbose reasoning suitable for debugging sessions.

                    Issue:
                    {message}

                    Reproduce symptoms, list hypotheses, narrow causes, and recommend fixes with verification steps.""";
            case "minimal-echo" ->
                    """
                    Echo the caller input verbatim.

                    Input:
                    {message}""";
            default ->
                    """
                    Address the request clearly and helpfully.

                    Request:
                    {message}""";
        };
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowPlannerPromptDefinition that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(promptTemplate, that.promptTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, promptTemplate);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String id;
        private String name;
        private String promptTemplate;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder promptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public WorkflowPlannerPromptDefinition build() {
            Objects.requireNonNull(id, "planner prompt id is required");
            Objects.requireNonNull(name, "planner prompt name is required");
            Objects.requireNonNull(promptTemplate, "planner prompt template is required");
            return new WorkflowPlannerPromptDefinition(this);
        }
    }
}
