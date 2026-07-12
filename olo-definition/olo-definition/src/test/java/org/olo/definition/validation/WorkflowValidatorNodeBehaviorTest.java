/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.validation;

import org.olo.definition.error.ErrorRoute;
import org.olo.definition.error.OnFailureDefinition;
import org.olo.definition.error.RetryPolicy;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowValidatorNodeBehaviorTest {

    @Test
    void acceptsHumanNodeWithApproval() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("hitl-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("trade-approval", NodeType.HUMAN)
                        .subtype("APPROVAL")
                        .approval(HumanApprovalDefinition.builder()
                                .title("Approve trade?")
                                .approvers(List.of("trading-desk"))
                                .build())
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsHumanNodeWithoutApproval() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("hitl-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("trade-approval", NodeType.HUMAN).build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("requires an approval block"));
    }

    @Test
    void acceptsOnFailureWithRetryAndRoute() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("openai", NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(3).build())
                                .route(ErrorRoute.builder().targetNodeId("fallback-model").build())
                                .build())
                        .build())
                .addNode(ValidationTestFixtures.node("fallback-model", NodeType.MODEL).build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsOnFailureRouteToUnknownNode() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("openai", NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .route(ErrorRoute.builder().targetNodeId("missing").build())
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown target node"));
    }

    @Test
    void rejectsOnFailureWithZeroAttempts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-retry")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(ValidationTestFixtures.node("openai", NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(0).build())
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("attempts must be >= 1"));
    }
}
