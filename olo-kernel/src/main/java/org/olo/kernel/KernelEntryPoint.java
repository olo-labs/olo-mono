/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.callback.UiCallbackReporter;
import org.olo.kernel.exception.KernelException;
import org.olo.core.tool.humaninput.HumanInputPluginOptions;
import org.olo.core.tool.humaninput.HumanInputSchemaResolver;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.human.HumanInputResumeSupport;
import org.olo.kernel.human.HumanResumeInput;
import org.olo.kernel.input.WorkflowReturnResolution;
import org.olo.kernel.input.WorkflowReturnResolver;
import org.olo.kernel.input.WorkflowInputMessages;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.TraversalResult;
import org.olo.kernel.traversal.engine.GraphTraversalEngine;
import org.olo.kernel.traversal.factory.GraphTraverserFactory;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kernel entry point for queue execution: context build, graph traversal, and UI callbacks.
 */
public final class KernelEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KernelEntryPoint.class);
    private static final GraphTraversalEngine TRAVERSAL_ENGINE = GraphTraverserFactory.defaultEngine();

    private KernelEntryPoint() {
    }

    /**
     * Builds kernel runtime context for {@code queue} from a deserialized {@link WorkflowInput}.
     *
     * @return the primary user message from the workflow input
     */
    public static String execute(String queue, WorkflowInput input, WorkflowDefinitionRegistry registry) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(registry, "registry");

        log.info(
                "Kernel entry: queue={}, workflowId={}, transactionId={}",
                queue,
                workflowIdFromInput(input),
                input.getRouting() != null ? input.getRouting().getTransactionId() : null);

        KernelExecutionSnapshot snapshot = buildContextAndNotifyUi(queue, input, registry);
        snapshot = traverse(snapshot);
        return reportWorkflowResult(snapshot);
    }

    /**
     * Builds kernel runtime context from a JSON payload string (file-based tests and legacy callers).
     */
    public static String execute(String queue, String inputPayload, WorkflowDefinitionRegistry registry) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(inputPayload, "inputPayload");
        Objects.requireNonNull(registry, "registry");

        WorkflowInput input = WorkflowInput.fromJson(inputPayload);
        WorkflowDefinition sourceGraph = registry.resolve(queue, workflowIdFromInput(input))
                .orElseThrow(() -> new KernelException("no workflow definition registered for queue: " + queue
                        + (workflowIdFromInput(input) != null ? " (workflowId=" + workflowIdFromInput(input) + ")" : "")));

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of(queue, input, sourceGraph));
        return finishFromContext(queue, context);
    }

    /**
     * Builds runtime context, validates graph readiness, and notifies the UI that execution started.
     * Does not traverse the graph.
     */
    public static KernelExecutionSnapshot buildContextAndNotifyUi(
            String queue, WorkflowInput input, WorkflowDefinitionRegistry registry) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(registry, "registry");

        WorkflowDefinition sourceGraph = registry.resolve(queue, workflowIdFromInput(input))
                .orElseThrow(() -> new KernelException("no workflow definition registered for queue: " + queue
                        + (workflowIdFromInput(input) != null ? " (workflowId=" + workflowIdFromInput(input) + ")" : "")));

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of(queue, input, sourceGraph));
        if (!context.isGraphReady()) {
            throw new KernelException("workflow graph is not ready for queue: " + queue);
        }

        UiCallbackReporter.reportContextReady(context);
        KernelExecutionSnapshot snapshot = KernelExecutionSnapshot.fromContext(context);
        TraversalDiagnostics.logContextReady(
                context,
                WorkflowInputMessages.primaryMessage(context.getInput()),
                snapshot.getNextActivityName());
        return snapshot;
    }

    /**
     * Notifies the UI that traversal is paused on a human-in-the-loop step.
     */
    public static void reportHumanWaiting(KernelExecutionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getStatus() != KernelExecutionSnapshot.Status.WAITING) {
            throw new KernelException("cannot report human waiting from snapshot status: " + snapshot.getStatus());
        }
        KernelRuntimeContext context = snapshot.toContext();
        String nodeId = snapshot.getLastNodeId() != null ? snapshot.getLastNodeId() : snapshot.getNextNodeId();
        if (nodeId == null || nodeId.isBlank()) {
            throw new KernelException("waiting snapshot has no human node id");
        }
        UiCallbackReporter.reportHumanWaiting(context, nodeId, snapshot.getStep(), humanWaitingOutput(context, nodeId));
    }

    private static Map<String, Object> humanWaitingOutput(KernelRuntimeContext context, String nodeId) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("approvalStatus", "waiting");
        output.put("message", context.getVariables().getString("message"));
        new DefaultGraphIndex(context.getGraph())
                .findNode(nodeId)
                .ifPresent(node -> enrichHumanWaitingOutput(output, node));
        return output;
    }

    private static void enrichHumanWaitingOutput(Map<String, Object> output, NodeDefinition node) {
        output.put("nodeId", node.getId());
        if (node.getSubtype() != null) {
            output.put("subtype", node.getSubtype());
        }
        if (node.getApproval() == null) {
            return;
        }
        if (node.getApproval().getTitle() != null) {
            output.put("title", node.getApproval().getTitle());
        }
        if (node.getApproval().getDescription() != null) {
            output.put("description", node.getApproval().getDescription());
        }
        if (!node.getApproval().getApprovers().isEmpty()) {
            output.put("approvers", node.getApproval().getApprovers());
        }
        if (node.getApproval().getTimeoutSeconds() != null) {
            output.put("timeoutSeconds", node.getApproval().getTimeoutSeconds());
        }
        output.put("requireCommentOnReject", node.getApproval().isRequireCommentOnReject());
        if (node.getApproval().getInputPluginId() != null) {
            HumanInputSchemaResolver.enrichWaitingOutput(output, node.getApproval().getInputPluginId());
        }
        ensureHumanWaitingOptions(output, node.getApproval().getInputPluginId());
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

    /**
     * Applies operator input and advances traversal past a paused {@code HUMAN} node.
     */
    public static KernelExecutionSnapshot resumeHumanInput(
            KernelExecutionSnapshot snapshot, HumanResumeInput input) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(input, "input");
        HumanResumeInput normalized = input.fields().isEmpty()
                ? HumanResumeInput.fromOperatorMessage(
                        input.comment() != null ? input.comment() : "", input.resolvedApprover())
                : input;
        return HumanInputResumeSupport.resume(snapshot, normalized);
    }

    /**
     * Executes one graph node scheduled as a dedicated Temporal activity.
     */
    public static KernelExecutionSnapshot executeTraversalStep(KernelExecutionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        requireRunning(snapshot);
        return TRAVERSAL_ENGINE.executeSingleStep(snapshot);
    }

    /**
     * Runs graph traversal to completion: dedicated activity per node, INLINE nodes in-process.
     */
    public static KernelExecutionSnapshot traverse(KernelExecutionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        KernelExecutionSnapshot current = snapshot;
        while (!current.isTerminal()) {
            current = executeTraversalStep(current);
        }
        logTraversalFinished(current);
        return current;
    }

    /**
     * Resolves the workflow return value and notifies the UI. Requires a terminal snapshot.
     */
    public static String reportWorkflowResult(KernelExecutionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getStatus() == KernelExecutionSnapshot.Status.FAILED) {
            throw new KernelException("workflow graph traversal failed for queue: " + snapshot.getQueue()
                    + (snapshot.getMessage() != null ? ": " + snapshot.getMessage() : ""));
        }
        if (snapshot.getStatus() != KernelExecutionSnapshot.Status.COMPLETED) {
            throw new KernelException("workflow graph traversal did not complete for queue: " + snapshot.getQueue());
        }

        KernelRuntimeContext context = snapshot.toContext();
        WorkflowReturnResolution resolution = WorkflowReturnResolver.resolveDetails(context);
        logReturnBeforeCallback(snapshot.getQueue(), context, resolution);
        UiCallbackReporter.reportWorkflowResult(
                context,
                resolution.returnVariableName(),
                resolution.returnVariableValue(),
                resolution.message(),
                resolution.usedAdminFallback());
        log.info(
                "Kernel entry complete: queue={}, workflowId={}, returnVariable={}, returnValue={}, messageLen={}, message={}",
                snapshot.getQueue(),
                workflowIdFromInput(context.getInput()),
                resolution.returnVariableName(),
                TraversalDiagnostics.formatValue(resolution.returnVariableValue()),
                resolution.message().length(),
                resolution.message());
        return resolution.message();
    }

    private static String finishFromContext(String queue, KernelRuntimeContext context) {
        KernelExecutionSnapshot snapshot = buildContextAndNotifyUiFromContext(queue, context);
        snapshot = traverse(snapshot);
        return reportWorkflowResult(snapshot);
    }

    private static KernelExecutionSnapshot buildContextAndNotifyUiFromContext(String queue, KernelRuntimeContext context) {
        if (!context.isGraphReady()) {
            throw new KernelException("workflow graph is not ready for queue: " + queue);
        }
        UiCallbackReporter.reportContextReady(context);
        KernelExecutionSnapshot snapshot = KernelExecutionSnapshot.fromContext(context);
        TraversalDiagnostics.logContextReady(
                context,
                WorkflowInputMessages.primaryMessage(context.getInput()),
                snapshot.getNextActivityName());
        return snapshot;
    }

    private static void requireRunning(KernelExecutionSnapshot snapshot) {
        if (snapshot.isTerminal()) {
            throw new KernelException("traversal snapshot is already terminal for queue: " + snapshot.getQueue());
        }
    }

    private static void logTraversalFinished(KernelExecutionSnapshot snapshot) {
        TraversalResult traversal = snapshot.toTraversalResult();
        if (snapshot.isWaiting()) {
            log.info(
                    "Traversal paused for human input: queue={}, lastNodeId={}, message={}",
                    snapshot.getQueue(),
                    traversal.lastNodeId(),
                    TraversalDiagnostics.formatValue(traversal.message()));
            return;
        }
        log.info(
                "Traversal finished: queue={}, completed={}, lastNodeId={}, lastNodeMessage={}",
                snapshot.getQueue(),
                traversal.completed(),
                traversal.lastNodeId(),
                TraversalDiagnostics.formatValue(traversal.message()));
        if (!traversal.completed()) {
            throw new KernelException("workflow graph traversal failed for queue: " + snapshot.getQueue()
                    + (traversal.message() != null ? ": " + traversal.message() : ""));
        }
    }

    private static void logReturnBeforeCallback(
            String queue, KernelRuntimeContext context, WorkflowReturnResolution resolution) {
        log.info(
                "Workflow return before callback: queue={}, returnVariable={}, returnValue={}, variables={}, message={}",
                queue,
                resolution.returnVariableName(),
                TraversalDiagnostics.formatValue(resolution.returnVariableValue()),
                context.getVariableMap(),
                resolution.message());
    }

    private static String workflowIdFromInput(WorkflowInput input) {
        if (input == null || input.getRouting() == null) {
            return null;
        }
        String pipeline = input.getRouting().getPipeline();
        return pipeline != null && !pipeline.isBlank() ? pipeline.trim() : null;
    }
}
