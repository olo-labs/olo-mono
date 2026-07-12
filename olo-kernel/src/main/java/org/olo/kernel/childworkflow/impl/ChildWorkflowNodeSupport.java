/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.childworkflow.impl;

import org.olo.definition.node.NodeDefinition;

final class ChildWorkflowNodeSupport {

    private ChildWorkflowNodeSupport() {
    }

    static String readChildWorkflowId(NodeDefinition node) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateAgentId");
            if (configured != null) {
                String delegateAgentId = String.valueOf(configured).trim();
                if (!delegateAgentId.isBlank()) {
                    return delegateAgentId;
                }
            }
        }
        if (node.getWorkflow() != null && node.getWorkflow().getWorkflowId() != null) {
            return node.getWorkflow().getWorkflowId().trim();
        }
        return null;
    }

    static String readDelegateMessage(NodeDefinition node) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateMessage");
            if (configured != null) {
                String message = String.valueOf(configured).trim();
                if (!message.isBlank()) {
                    return message;
                }
            }
        }
        return null;
    }
}
