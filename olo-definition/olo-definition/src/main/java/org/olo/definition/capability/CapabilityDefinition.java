/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.capability;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

/**
 * Planner-readable contract shared by workflows, tools, agents, and nodes.
 * Runtime graphs and {@code configuration} stay separate; planners consume only capability metadata.
 * <p>
 * {@link #getName()} is the capability descriptor title for planners and orchestrators — not the
 * workflow Studio label ({@link org.olo.definition.workflow.WorkflowDefinition#getLabel()}). The two
 * often match on presets (e.g. both {@code "Planner"}) but serve different layers: UI display vs
 * capability contract.
 * <p>
 * {@link #getRequiredInputs()} / {@link #getRequiredOutputs()} are semantic planner contracts — not the
 * same as workflow invocation {@code inputs} on {@link org.olo.definition.workflow.WorkflowDefinition}.
 */
@JsonDeserialize(builder = CapabilityDefinitionBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CapabilityDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final List<String> requiredInputs;
    private final List<String> requiredOutputs;
    private final List<String> tags;
    private final List<String> examples;
    private final Double cost;
    private final Double latency;
    private final Double confidence;
    private final List<String> toolRequirements;
    private final List<String> requiredContext;

    CapabilityDefinition(CapabilityDefinitionBuilder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.requiredInputs = builder.requiredInputs == null ? List.of() : List.copyOf(builder.requiredInputs);
        this.requiredOutputs = builder.requiredOutputs == null ? List.of() : List.copyOf(builder.requiredOutputs);
        this.tags = builder.tags == null ? List.of() : List.copyOf(builder.tags);
        this.examples = builder.examples == null ? List.of() : List.copyOf(builder.examples);
        this.cost = builder.cost;
        this.latency = builder.latency;
        this.confidence = builder.confidence;
        this.toolRequirements =
                builder.toolRequirements == null ? List.of() : List.copyOf(builder.toolRequirements);
        this.requiredContext =
                builder.requiredContext == null ? List.of() : List.copyOf(builder.requiredContext);
    }

    public static CapabilityDefinitionBuilder builder() {
        return new CapabilityDefinitionBuilder();
    }

    public String getId() {
        return id;
    }

    /** Capability descriptor title (planner contract) — not {@link org.olo.definition.workflow.WorkflowDefinition#getLabel()}. */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("required_inputs")
    public List<String> getRequiredInputs() {
        return requiredInputs;
    }

    @JsonProperty("required_outputs")
    public List<String> getRequiredOutputs() {
        return requiredOutputs;
    }

    /** @deprecated use {@link #getRequiredInputs()} */
    @Deprecated
    @JsonIgnore
    public List<String> getInputs() {
        return requiredInputs;
    }

    /** @deprecated use {@link #getRequiredOutputs()} */
    @Deprecated
    @JsonIgnore
    public List<String> getOutputs() {
        return requiredOutputs;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getExamples() {
        return examples;
    }

    public Double getCost() {
        return cost;
    }

    @JsonProperty("latency")
    public Double getLatency() {
        return latency;
    }

    public Double getConfidence() {
        return confidence;
    }

    @JsonProperty("tool_requirements")
    public List<String> getToolRequirements() {
        return toolRequirements;
    }

    @JsonProperty("required_context")
    public List<String> getRequiredContext() {
        return requiredContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CapabilityDefinition that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(requiredInputs, that.requiredInputs)
                && Objects.equals(requiredOutputs, that.requiredOutputs)
                && Objects.equals(tags, that.tags)
                && Objects.equals(examples, that.examples)
                && Objects.equals(cost, that.cost)
                && Objects.equals(latency, that.latency)
                && Objects.equals(confidence, that.confidence)
                && Objects.equals(toolRequirements, that.toolRequirements)
                && Objects.equals(requiredContext, that.requiredContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                description,
                requiredInputs,
                requiredOutputs,
                tags,
                examples,
                cost,
                latency,
                confidence,
                toolRequirements,
                requiredContext);
    }
}
