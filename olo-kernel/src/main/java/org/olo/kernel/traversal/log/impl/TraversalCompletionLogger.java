/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.log.impl;

import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.spi.node.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Logs traversal completion, failure, and return-value resolution.
 */
public final class TraversalCompletionLogger {

    private static final Logger log = LoggerFactory.getLogger(TraversalCompletionLogger.class);

    private static final String STUB_HINT =
            "AGENT nodes currently use olo-core AgentNode child-workflow-stub; "
                    + "real model/child-workflow dispatch is not wired yet";

    private TraversalCompletionLogger() {
    }

    public static void logTraversalComplete(
            String lastNodeId,
            String lastMessage,
            Map<String, Object> variables,
            Map<String, ExecutionOutput> outputs) {
        Object returnValue = variables != null ? variables.get("ReturnValue") : null;
        log.info(
                "Traversal complete: lastNodeId={}, lastNodeMessage={}, returnValue={}, variables={}, outputKeys={}",
                lastNodeId,
                formatValue(lastMessage),
                formatValue(returnValue),
                variables,
                outputs != null ? outputs.keySet() : null);
        if (returnValue != null && String.valueOf(returnValue).contains("child workflow dispatch pending")) {
            log.warn(
                    "Traversal produced stub AGENT response in ReturnValue; hint={}",
                    STUB_HINT);
        }
    }

    public static void logTraversalFailed(String nodeId, NodeStatus status, String message) {
        log.error("Traversal failed: nodeId={}, status={}, message={}", nodeId, status, formatValue(message));
    }

    public static void logTraversalWaiting(String nodeId, String message) {
        log.info("Traversal waiting for human input: nodeId={}, message={}", nodeId, formatValue(message));
    }

    public static void logReturnResolve(
            String path,
            String returnVariable,
            Object returnValue,
            String message,
            boolean usedAdminFallback) {
        log.info(
                "Return resolve: path={}, returnVariable={}, returnValue={}, message={}, adminFallback={}",
                path,
                returnVariable,
                formatValue(returnValue),
                formatValue(message),
                usedAdminFallback);
        if (message != null && message.contains("child workflow dispatch pending")) {
            log.warn("Return resolve stub message returned to caller; hint={}", STUB_HINT);
        }
    }

    public static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200) + "...";
    }
}
