/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.log.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs traversal bootstrap context before the first step executes.
 */
public final class TraversalContextLogger {

    private static final Logger log = LoggerFactory.getLogger(TraversalContextLogger.class);

    private TraversalContextLogger() {
    }

    public static void logContextReady(
            KernelRuntimeContext context, String primaryInputMessage, String nextActivityName) {
        log.info(
                "Traversal context ready: queue={}, workflowId={}, version={}, transactionId={}, graphReady={}, "
                        + "nextActivityName={}, primaryInputMessage={}, variables={}",
                context.getQueue(),
                context.getGraph().getId(),
                context.getGraph().getVersion(),
                transactionIdFrom(context),
                context.isGraphReady(),
                nextActivityName,
                TraversalCompletionLogger.formatValue(primaryInputMessage),
                context.getVariableMap());
    }

    public static void logContextReady(KernelRuntimeContext context, String primaryInputMessage) {
        logContextReady(context, primaryInputMessage, null);
    }

    public static void logTraversalStart(
            KernelRuntimeContext context, int nodeCount, int edgeCount, String startNodeId) {
        log.info(
                "Traversal start: queue={}, workflowId={}, transactionId={}, nodes={}, edges={}, startNodeId={}",
                context.getQueue(),
                context.getGraph().getId(),
                transactionIdFrom(context),
                nodeCount,
                edgeCount,
                startNodeId);
    }

    private static String transactionIdFrom(KernelRuntimeContext context) {
        if (context.getInput() == null || context.getInput().getRouting() == null) {
            return null;
        }
        String transactionId = context.getInput().getRouting().getTransactionId();
        if (transactionId == null || transactionId.isBlank()) {
            return null;
        }
        return transactionId.trim();
    }
}
