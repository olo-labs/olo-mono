/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowValidatorStateAccessTest {

    @Test
    void rejectsWriteToUnknownStateField() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typo-write")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(ValidationTestFixtures.node("research", NodeType.TOOL)
                        .addWrite("state.analysys")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("writes unknown state field: state.analysys"));
        assertThat(result.errors()).anyMatch(e -> e.contains("no state field 'analysys'"));
    }

    @Test
    void rejectsReadFromUnknownStateField() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typo-read")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(ValidationTestFixtures.node("risk", NodeType.TOOL)
                        .addRead("state.analysys")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("reads unknown state field: state.analysys"));
    }

    @Test
    void rejectsStateReadWhenFieldNotDeclared() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("no-state-schema")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("agent", NodeType.TOOL)
                        .addRead("state.symbol")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("reads unknown state field: state.symbol"));
    }

    @Test
    void acceptsNestedAndIndexedStatePaths() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("nested-state")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .putState("news", StateFieldDefinition.builder().schema("News[]").build())
                .addNode(ValidationTestFixtures.node("scorer", NodeType.TOOL)
                        .addRead("state.analysis.score")
                        .addRead("state.news[0]")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void acceptsInputAndParameterReadPaths() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("input-param-reads")
                .capability(ValidationTestFixtures.minimalCapability())
                .putInput("symbol", WorkflowInputDefinition.builder().schema("String").required(true).build())
                .putParameter("temperature", WorkflowParameterDefinition.builder().schema("number").build())
                .addNode(ValidationTestFixtures.node("llm", NodeType.MODEL)
                        .addRead("input.symbol")
                        .addRead("parameter.temperature")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsWriteToInputOrParameter() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("bad-write-root")
                .capability(ValidationTestFixtures.minimalCapability())
                .putInput("symbol", WorkflowInputDefinition.builder().schema("String").build())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(ValidationTestFixtures.node("bad", NodeType.TOOL)
                        .addWrite("input.symbol")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("write path must use state. namespace"));
    }

    @Test
    void acceptsValidReadsAndWrites() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("valid-state-access")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("symbol", StateFieldDefinition.builder().schema("String").build())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(ValidationTestFixtures.node("research", NodeType.TOOL)
                        .addRead("state.symbol")
                        .addWrite("state.analysis")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }
}
