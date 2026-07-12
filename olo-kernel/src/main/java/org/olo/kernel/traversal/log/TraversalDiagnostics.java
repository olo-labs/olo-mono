/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.log;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.traversal.log.impl.TraversalCompletionLogger;
import org.olo.kernel.traversal.log.impl.TraversalContextLogger;
import org.olo.kernel.traversal.log.impl.TraversalStepLogger;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.Map;

/**
 * Facade for structured traversal diagnostics. Implementation is split across focused loggers in
 * {@link org.olo.kernel.traversal.log.impl} to keep each class under 200 lines.
 */
public final class TraversalDiagnostics {

    private TraversalDiagnostics() {
    }

    public static void logContextReady(
            KernelRuntimeContext context, String primaryInputMessage, String nextActivityName) {
        TraversalContextLogger.logContextReady(context, primaryInputMessage, nextActivityName);
    }

    public static void logContextReady(KernelRuntimeContext context, String primaryInputMessage) {
        TraversalContextLogger.logContextReady(context, primaryInputMessage);
    }

    public static void logTraversalStart(
            KernelRuntimeContext context, int nodeCount, int edgeCount, String startNodeId) {
        TraversalContextLogger.logTraversalStart(context, nodeCount, edgeCount, startNodeId);
    }

    public static void logStepEnter(int step, NodeDefinition node, Map<String, Object> variables) {
        TraversalStepLogger.logStepEnter(step, node, variables);
    }

    public static void logStepEnter(int step, String nodeId, String nodeType, Map<String, Object> variables) {
        TraversalStepLogger.logStepEnter(step, nodeId, nodeType, variables);
    }

    public static void logInputBind(
            String nodeId, String variableName, String extractedMessage, boolean applied, String reason) {
        TraversalStepLogger.logInputBind(nodeId, variableName, extractedMessage, applied, reason);
    }

    public static void logAgentExecutorSelected(String nodeId, String executorId) {
        TraversalStepLogger.logAgentExecutorSelected(nodeId, executorId);
    }

    public static void logLlmInvocation(String nodeId, LlmInvocationResult invocation) {
        TraversalStepLogger.logLlmInvocation(nodeId, invocation);
    }

    public static void logLlmFailure(String nodeId, String message) {
        TraversalStepLogger.logLlmFailure(nodeId, message);
    }

    public static void logNodeRequest(
            String nodeId,
            String definitionNodeType,
            String spiNodeType,
            Map<String, Object> input,
            Map<String, Object> configuration) {
        TraversalStepLogger.logNodeRequest(nodeId, definitionNodeType, spiNodeType, input, configuration);
    }

    public static void logNodeResult(String nodeId, String nodeType, NodeResult result) {
        TraversalStepLogger.logNodeResult(nodeId, nodeType, result);
    }

    public static void logExecutionOutput(String nodeId, String outputKey, ExecutionOutput output) {
        TraversalStepLogger.logExecutionOutput(nodeId, outputKey, output);
    }

    public static void logOutputApply(
            String nodeId,
            String returnVariable,
            String action,
            Object appliedValue,
            String reason) {
        TraversalStepLogger.logOutputApply(nodeId, returnVariable, action, appliedValue, reason);
    }

    public static void logExecutionDecision(String nodeId, ExecutionDecision decision) {
        TraversalStepLogger.logExecutionDecision(nodeId, decision);
    }

    public static void logStepExit(int step, String nodeId, String nextNodeId, Map<String, Object> variables) {
        TraversalStepLogger.logStepExit(step, nodeId, nextNodeId, variables);
    }

    public static void logTraversalComplete(
            String lastNodeId,
            String lastMessage,
            Map<String, Object> variables,
            Map<String, ExecutionOutput> outputs) {
        TraversalCompletionLogger.logTraversalComplete(lastNodeId, lastMessage, variables, outputs);
    }

    public static void logTraversalFailed(String nodeId, NodeStatus status, String message) {
        TraversalCompletionLogger.logTraversalFailed(nodeId, status, message);
    }

    public static void logReturnResolve(
            String path,
            String returnVariable,
            Object returnValue,
            String message,
            boolean usedAdminFallback) {
        TraversalCompletionLogger.logReturnResolve(path, returnVariable, returnValue, message, usedAdminFallback);
    }

    public static String formatValue(Object value) {
        return TraversalCompletionLogger.formatValue(value);
    }
}
