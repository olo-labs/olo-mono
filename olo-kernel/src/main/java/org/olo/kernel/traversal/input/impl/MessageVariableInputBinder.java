/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.input.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.input.WorkflowInputMessages;
import org.olo.kernel.traversal.input.WorkflowInputBinder;
import org.olo.kernel.traversal.log.TraversalDiagnostics;

public final class MessageVariableInputBinder implements WorkflowInputBinder {

    public static final String MESSAGE_VARIABLE = "message";

    @Override
    public void bind(KernelRuntimeContext context) {
        String message = WorkflowInputMessages.primaryMessage(context.getInput());
        if (message.isBlank()) {
            TraversalDiagnostics.logInputBind(
                    "start",
                    MESSAGE_VARIABLE,
                    message,
                    false,
                    "no non-blank user message found in WorkflowInput");
            bindRagTagFromMetadata(context);
            return;
        }
        if (!context.getVariables().has(MESSAGE_VARIABLE)) {
            TraversalDiagnostics.logInputBind(
                    "start",
                    MESSAGE_VARIABLE,
                    message,
                    false,
                    "graph does not declare variable '" + MESSAGE_VARIABLE + "'");
            bindRagTagFromMetadata(context);
            return;
        }
        context.getVariables().set(MESSAGE_VARIABLE, message);
        TraversalDiagnostics.logInputBind("start", MESSAGE_VARIABLE, message, true, null);
        bindRagTagFromMetadata(context);
    }

    private static void bindRagTagFromMetadata(KernelRuntimeContext context) {
        if (context.getInput() == null || context.getInput().getMetadata() == null) {
            return;
        }
        String ragTag = context.getInput().getMetadata().getRagTag();
        if (ragTag == null || ragTag.isBlank()) {
            return;
        }
        String trimmed = ragTag.trim();
        if (context.getVariables().has("ragTag")) {
            context.getVariables().set("ragTag", trimmed);
        }
        if (context.getVariables().has("capabilitySource")) {
            context.getVariables().set("capabilitySource", trimmed);
        }
    }
}
