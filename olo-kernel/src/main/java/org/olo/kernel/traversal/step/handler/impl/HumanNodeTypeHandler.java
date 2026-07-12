/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.olo.core.tool.humaninput.HumanInputPluginOptions;
import org.olo.core.tool.humaninput.HumanInputSchemaResolver;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.spi.node.NodeResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes canvas {@code HUMAN} nodes by pausing traversal until an operator submits input via the UI.
 */
public final class HumanNodeTypeHandler implements NodeTypeHandler {

    @Override
    public boolean supports(String nodeType) {
        return NodeType.HUMAN.name().equals(nodeType);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        HumanApprovalDefinition approval = node.getApproval();
        context.getVariables().set("approvalStatus", "waiting");

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("approvalStatus", "waiting");
        output.put("nodeId", node.getId());
        if (node.getSubtype() != null) {
            output.put("subtype", node.getSubtype());
        }
        if (approval != null) {
            if (approval.getTitle() != null) {
                output.put("title", approval.getTitle());
            }
            if (approval.getDescription() != null) {
                output.put("description", approval.getDescription());
            }
            if (!approval.getApprovers().isEmpty()) {
                output.put("approvers", approval.getApprovers());
            }
            if (approval.getTimeoutSeconds() != null) {
                output.put("timeoutSeconds", approval.getTimeoutSeconds());
            }
            output.put("requireCommentOnReject", approval.isRequireCommentOnReject());
            if (approval.getInputPluginId() != null) {
                output.put("inputPluginId", approval.getInputPluginId());
                HumanInputSchemaResolver.enrichWaitingOutput(output, approval.getInputPluginId());
            }
        }
        ensureHumanWaitingOptions(output, approval != null ? approval.getInputPluginId() : null);

        String prompt = approval != null && approval.getTitle() != null
                ? approval.getTitle()
                : "Human input required";
        TraversalDiagnostics.logNodeRequest(node.getId(), node.getType(), "HUMAN", Map.of(), output);
        return NodeResult.waiting(prompt, output);
    }

    private static void ensureHumanWaitingOptions(Map<String, Object> output, String inputPluginId) {
        Object existing = output.get("options");
        if (existing instanceof List<?> list && !list.isEmpty()) {
            return;
        }
        output.put("options", HumanInputPluginOptions.optionsFor(inputPluginId));
        if (output.get("inputType") == null) {
            output.put("inputType", output.get("inputPluginId") != null ? "plugin" : "options");
        }
    }
}
