/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.capability;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jackson builder and fluent factory for {@link CapabilityDefinition}.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class CapabilityDefinitionBuilder {

    String id;
    String name;
    String description;
    @JsonProperty("required_inputs")
    @JsonAlias("inputs")
    List<String> requiredInputs;
    @JsonProperty("required_outputs")
    @JsonAlias("outputs")
    List<String> requiredOutputs;
    List<String> tags;
    List<String> examples;
    Double cost;
    Double latency;
    Double confidence;
    List<String> toolRequirements;
    @JsonProperty("required_context")
    List<String> requiredContext;

    public CapabilityDefinitionBuilder id(String id) {
        this.id = id;
        return this;
    }

    public CapabilityDefinitionBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CapabilityDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CapabilityDefinitionBuilder requiredInputs(List<String> requiredInputs) {
        this.requiredInputs = requiredInputs;
        return this;
    }

    public CapabilityDefinitionBuilder addRequiredInput(String input) {
        if (this.requiredInputs == null) {
            this.requiredInputs = new ArrayList<>();
        }
        this.requiredInputs.add(input);
        return this;
    }

    /** @deprecated use {@link #addRequiredInput(String)} */
    @Deprecated
    public CapabilityDefinitionBuilder addInput(String input) {
        return addRequiredInput(input);
    }

    /** @deprecated use {@link #requiredInputs(List)} */
    @Deprecated
    public CapabilityDefinitionBuilder inputs(List<String> inputs) {
        this.requiredInputs = inputs;
        return this;
    }

    public CapabilityDefinitionBuilder requiredOutputs(List<String> requiredOutputs) {
        this.requiredOutputs = requiredOutputs;
        return this;
    }

    public CapabilityDefinitionBuilder addRequiredOutput(String output) {
        if (this.requiredOutputs == null) {
            this.requiredOutputs = new ArrayList<>();
        }
        this.requiredOutputs.add(output);
        return this;
    }

    /** @deprecated use {@link #addRequiredOutput(String)} */
    @Deprecated
    public CapabilityDefinitionBuilder addOutput(String output) {
        return addRequiredOutput(output);
    }

    /** @deprecated use {@link #requiredOutputs(List)} */
    @Deprecated
    public CapabilityDefinitionBuilder outputs(List<String> outputs) {
        this.requiredOutputs = outputs;
        return this;
    }

    public CapabilityDefinitionBuilder tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public CapabilityDefinitionBuilder addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
        return this;
    }

    public CapabilityDefinitionBuilder examples(List<String> examples) {
        this.examples = examples;
        return this;
    }

    public CapabilityDefinitionBuilder addExample(String example) {
        if (this.examples == null) {
            this.examples = new ArrayList<>();
        }
        this.examples.add(example);
        return this;
    }

    public CapabilityDefinitionBuilder cost(Double cost) {
        this.cost = cost;
        return this;
    }

    public CapabilityDefinitionBuilder latency(Double latency) {
        this.latency = latency;
        return this;
    }

    public CapabilityDefinitionBuilder confidence(Double confidence) {
        this.confidence = confidence;
        return this;
    }

    public CapabilityDefinitionBuilder toolRequirements(List<String> toolRequirements) {
        this.toolRequirements = toolRequirements;
        return this;
    }

    public CapabilityDefinitionBuilder addToolRequirement(String toolRequirement) {
        if (this.toolRequirements == null) {
            this.toolRequirements = new ArrayList<>();
        }
        this.toolRequirements.add(toolRequirement);
        return this;
    }

    public CapabilityDefinitionBuilder requiredContext(List<String> requiredContext) {
        this.requiredContext = requiredContext;
        return this;
    }

    public CapabilityDefinitionBuilder addRequiredContext(String contextKey) {
        if (this.requiredContext == null) {
            this.requiredContext = new ArrayList<>();
        }
        this.requiredContext.add(contextKey);
        return this;
    }

    public CapabilityDefinition build() {
        Objects.requireNonNull(name, "capability name is required");
        Objects.requireNonNull(description, "capability description is required");
        return new CapabilityDefinition(this);
    }
}
