/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowValidatorTest {

    @Test
    void acceptsValidWorkflow() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Valid")
                .capability(ValidationTestFixtures.minimalCapability())
                .inputNode("in")
                .outputNode("out")
                .connect("in", "out")
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
        WorkflowValidator.validateOrThrow(workflow);
    }

    @Test
    void validateOrThrowThrows() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .capability(ValidationTestFixtures.minimalCapability())
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        assertThatThrownBy(() -> WorkflowValidator.validateOrThrow(workflow))
                .isInstanceOf(WorkflowValidationException.class);
    }
}
