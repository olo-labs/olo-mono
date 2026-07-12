/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.human;

import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.spi.node.NodeStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HumanInputResumeSupportTest {

    @Test
    void resumesWaitingSnapshotAndAdvancesToNextNode() {
        WorkflowDefinition graph = WorkflowBuilder.create("Performance triage")
                .id("performance-triage-orchestrator")
                .startNode("start")
                .humanNode(
                        "human-input",
                        HumanApprovalDefinition.builder()
                                .title("Approve container restart")
                                .description("Provide containerId and namespace")
                                .approvers(List.of("operator"))
                                .build())
                .modelNode("planner", "CHAT")
                .connect("start", "human-input")
                .connect("human-input", "planner")
                .build();

        KernelRuntimeContext context = new KernelRuntimeContext(
                "performance-triage-orchestrator",
                new WorkflowInput("1.0", List.of(), null, null, null, null),
                graph,
                true,
                WorkflowRuntimeVariables.fromMap(Map.of("message", "Investigate latency spike")));

        KernelExecutionSnapshot waiting = KernelExecutionSnapshot.fromContext(
                context,
                "human-input",
                2,
                KernelExecutionSnapshot.Status.WAITING,
                "human-input",
                NodeStatus.WAITING,
                "Approve container restart");

        KernelExecutionSnapshot resumed = HumanInputResumeSupport.resume(
                waiting,
                HumanResumeInput.of(
                        "Mock restart-container arguments: {\"containerId\":\"payment-api-7f8c9d\",\"namespace\":\"production\"}",
                        "operator"));

        assertThat(resumed.getStatus()).isEqualTo(KernelExecutionSnapshot.Status.RUNNING);
        assertThat(resumed.getNextNodeId()).isEqualTo("planner");
        assertThat(resumed.getVariables().get("humanInputApproved")).isEqualTo(true);
        assertThat(resumed.getVariables().get("approvalStatus")).isEqualTo("approved");
        assertThat(String.valueOf(resumed.getVariables().get("message"))).contains("payment-api-7f8c9d");
    }
}
