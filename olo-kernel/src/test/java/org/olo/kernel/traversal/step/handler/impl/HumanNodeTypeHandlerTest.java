/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.input.model.WorkflowInput;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;
import org.olo.spi.node.NodeStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HumanNodeTypeHandlerTest {

    @Test
    void pausesPerformanceTriageHumanInputWithApprovalMetadata() {
        HumanNodeTypeHandler handler = new HumanNodeTypeHandler();
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromMap(
                java.util.Map.of("message", "Investigate latency spike"));
        KernelRuntimeContext context = new KernelRuntimeContext(
                "performance-triage-orchestrator",
                new WorkflowInput("1.0", java.util.List.of(), null, null, null, null),
                WorkflowDefinition.builder().id("performance-triage-orchestrator").build(),
                true,
                variables);

        NodeDefinition humanNode = NodeDefinition.builder()
                .id("human-input")
                .type(NodeType.HUMAN.name())
                .approval(HumanApprovalDefinition.builder()
                        .title("Approve container restart")
                        .description("Provide containerId and namespace")
                        .approvers(java.util.List.of("operator"))
                        .build())
                .build();

        var result = handler.execute(context, humanNode);

        assertThat(result.status()).isEqualTo(NodeStatus.WAITING);
        assertThat(variables.getString("message")).isEqualTo("Investigate latency spike");
        assertThat(variables.get("approvalStatus")).isEqualTo("waiting");
        assertThat(result.output()).containsEntry("approvalStatus", "waiting");
        assertThat(result.output()).containsEntry("title", "Approve container restart");
        assertThat(result.output()).containsEntry("description", "Provide containerId and namespace");
    }
}
