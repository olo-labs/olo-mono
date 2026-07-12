/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.kernel.traversal.log.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.agent.LlmInvocationResult;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.traversal.scheduling.NodeActivityNaming;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Logs per-step node execution, binding, and strategy decisions.
 */
public final class TraversalStepLogger {

    private static final Logger log = LoggerFactory.getLogger(TraversalStepLogger.class);

    private static final String STUB_HINT =
            "AGENT nodes currently use olo-core AgentNode child-workflow-stub; "
                    + "real model/child-workflow dispatch is not wired yet";

    private TraversalStepLogger() {
    }

    public static void logStepEnter(int step, NodeDefinition node, Map<String, Object> variables) {
        log.info(
                "Traversal step {} enter: nodeId={}, nodeType={}, nodeLabel={}, activityName={}, variables={}",
                step,
                node.getId(),
                node.getType(),
                resolveNodeLabel(node),
                NodeActivityNaming.formatNode(node),
                variables);
    }

    public static void logStepEnter(int step, String nodeId, String nodeType, Map<String, Object> variables) {
        log.info("Traversal step {} enter: nodeId={}, nodeType={}, variables={}", step, nodeId, nodeType, variables);
    }

    public static void logInputBind(
            String nodeId, String variableName, String extractedMessage, boolean applied, String reason) {
        if (applied) {
            log.info(
                    "Traversal step input bind: nodeId={}, variable={}, value={}",
                    nodeId,
                    variableName,
                    TraversalCompletionLogger.formatValue(extractedMessage));
        } else {
            log.info(
                    "Traversal step input bind skipped: nodeId={}, variable={}, extractedMessage={}, reason={}",
                    nodeId,
                    variableName,
                    TraversalCompletionLogger.formatValue(extractedMessage),
                    reason);
        }
    }

    public static void logAgentExecutorSelected(String nodeId, String executorId) {
        log.info("Traversal step agent executor: nodeId={}, executorId={}", nodeId, executorId);
    }

    public static void logLlmInvocation(String nodeId, LlmInvocationResult invocation) {
        log.info(
                "Traversal step LLM invoke: nodeId={}, providerId={}, model={}, baseUrl={}, temperature={}, "
                        + "renderedPrompt={}, response={}",
                nodeId,
                invocation.modelCall().providerId(),
                invocation.modelCall().model(),
                invocation.modelCall().baseUrl(),
                invocation.modelCall().temperature(),
                TraversalCompletionLogger.formatValue(invocation.renderedPrompt()),
                TraversalCompletionLogger.formatValue(invocation.response()));
    }

    public static void logLlmFailure(String nodeId, String message) {
        log.error("Traversal step LLM failed: nodeId={}, message={}", nodeId, TraversalCompletionLogger.formatValue(message));
    }

    public static void logNodeRequest(
            String nodeId,
            String definitionNodeType,
            String spiNodeType,
            Map<String, Object> input,
            Map<String, Object> configuration) {
        log.info(
                "Traversal step node request: nodeId={}, definitionType={}, spiType={}, input={}, configuration={}",
                nodeId,
                definitionNodeType,
                spiNodeType,
                input,
                configuration);
    }

    public static void logNodeResult(String nodeId, String nodeType, NodeResult result) {
        if (result == null) {
            log.warn("Traversal step node result: nodeId={}, nodeType={}, result=null", nodeId, nodeType);
            return;
        }
        log.info(
                "Traversal step node result: nodeId={}, nodeType={}, status={}, message={}, output={}",
                nodeId,
                nodeType,
                result.status(),
                TraversalCompletionLogger.formatValue(result.message()),
                result.output());
        if (result.status() == NodeStatus.COMPLETED
                && result.message() != null
                && result.message().contains("child workflow dispatch pending")) {
            log.warn(
                    "Traversal step stub response detected: nodeId={}, nodeType={}, hint={}",
                    nodeId,
                    nodeType,
                    STUB_HINT);
        }
    }

    public static void logExecutionOutput(String nodeId, String outputKey, ExecutionOutput output) {
        log.info(
                "Traversal step execution output: nodeId={}, outputKey={}, nodeType={}, value={}",
                nodeId,
                outputKey,
                output.nodeType(),
                TraversalCompletionLogger.formatValue(output.asReturnMessage()));
    }

    public static void logOutputApply(
            String nodeId,
            String returnVariable,
            String action,
            Object appliedValue,
            String reason) {
        if (appliedValue != null) {
            log.info(
                    "Traversal step output apply: nodeId={}, returnVariable={}, action={}, value={}",
                    nodeId,
                    returnVariable,
                    action,
                    TraversalCompletionLogger.formatValue(appliedValue));
        } else {
            log.info(
                    "Traversal step output apply skipped: nodeId={}, returnVariable={}, reason={}",
                    nodeId,
                    returnVariable,
                    reason);
        }
    }

    public static void logExecutionDecision(String nodeId, ExecutionDecision decision) {
        log.info(
                "Traversal execution decision: nodeId={}, strategy={}, kind={}, nextNodeId={}, branches={}, joinNodeId={}",
                nodeId,
                decision.strategyName(),
                decision.kind(),
                decision.nextNodeId().orElse(null),
                decision.branchEntryNodeIds(),
                decision.joinNodeId().orElse(null));
    }

    public static void logStepExit(int step, String nodeId, String nextNodeId, Map<String, Object> variables) {
        log.info(
                "Traversal step {} exit: nodeId={}, nextNodeId={}, variables={}",
                step,
                nodeId,
                nextNodeId != null ? nextNodeId : "<end>",
                variables);
    }

    private static String resolveNodeLabel(NodeDefinition node) {
        if (node.getLabel() != null && !node.getLabel().isBlank()) {
            return node.getLabel().trim();
        }
        return null;
    }
}
