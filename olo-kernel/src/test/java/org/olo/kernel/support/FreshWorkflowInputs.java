/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.support;

import org.olo.input.model.WorkflowInput;

/** Self-contained input for clean-checkout kernel tests; no generated sample files. */
public final class FreshWorkflowInputs {
    private FreshWorkflowInputs() {}

    public static org.olo.definition.workflow.WorkflowDefinition graph(String id) {
        var node = org.olo.definition.node.NodeDefinition.builder()
                .id("agent").label("Agent").type("AGENT")
                .executionKind(org.olo.definition.execution.ExecutionKind.ACTIVITY)
                .executionModel(org.olo.definition.execution.ExecutionModel.INLINE)
                .workflow(org.olo.definition.workflow.WorkflowReferenceDefinition.builder()
                        .workflowId(id).version("1.0.0").build())
                .build();
        return org.olo.definition.workflow.WorkflowBuilder.create("Fresh agent")
                .id(id).queue("oloQueue2").version("1.0.0")
                .startNodeWithMessageInput("start").addNode(node).endNode("end")
                .connect("start", "agent").connect("agent", "end").build();
    }

    public static org.olo.bootstrap.registry.WorkflowDefinitionRegistry registry(String id) {
        return org.olo.bootstrap.registry.WorkflowDefinitionRegistry.of(java.nio.file.Path.of("."),
                java.util.List.of(new org.olo.bootstrap.model.CachedWorkflowDefinition(id + ".json", graph(id))));
    }

    public static WorkflowInput minimal() throws java.io.IOException {
        return WorkflowInput.fromJson("""
                {"version":"1.0","inputs":[{"name":"query","displayName":"Query",
                  "type":"STRING","storage":{"mode":"LOCAL"},"value":"Hello, OLO"}],
                  "routing":{"pipeline":"default-pipeline","transactionType":"QUESTION_ANSWER",
                  "transactionId":"fresh-kernel-entry"}}
                """);
    }
}
