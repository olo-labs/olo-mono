/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.input.model.Context;
import org.olo.input.model.Execution;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.exception.KernelContextException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Delivers execution state to the olo backend using the callback URL from the workflow input.
 */
public final class UiCallbackReporter {

    private static final Logger log = LoggerFactory.getLogger(UiCallbackReporter.class);

    private static final long CONTEXT_READY_SEQUENCE = 1L;
    private static final long WORKFLOW_RESULT_SEQUENCE = 2L;
    private static final int EVENT_VERSION = 1;
    private static final String KERNEL_NODE_ID = "kernel";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private UiCallbackReporter() {
    }

    /**
     * Posts the initial context-ready state to POST /api/runs/{runId}/events on the callback base URL.
     * No-op when no callback URL is present.
     */
    public static void reportContextReady(KernelRuntimeContext context) {
        Objects.requireNonNull(context, "context");

        if (resolveCallbackUrl(context.getInput()) == null) {
            return;
        }

        Context inputContext = context.getInput().getContext();
        String runId = inputContext != null ? inputContext.getRunId() : null;
        if (runId == null || runId.isBlank()) {
            throw new KernelContextException("runId is required to deliver UI callback");
        }

        String eventPostUrl = resolveEventPostUrl(context.getInput(), runId);
        String correlationId = inputContext.getCorrelationId();
        RunEventCallbackPayload payload = new RunEventCallbackPayload(
                CONTEXT_READY_SEQUENCE,
                EVENT_VERSION,
                "NODE_COMPLETED",
                correlationId,
                KERNEL_NODE_ID,
                "root",
                "SYSTEM",
                "COMPLETED",
                Map.of(
                        "status", "CONTEXT_READY",
                        "queue", context.getQueue(),
                        "graphReady", context.isGraphReady(),
                        "variables", context.getVariableMap()),
                Map.of("phase", "kernel-context"));

        postPayload(eventPostUrl, payload);
    }

    private static void postPayload(String eventPostUrl, RunEventCallbackPayload payload) {
        try {
            String body = MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventPostUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (isSuccessfulCallbackStatus(response.statusCode())) {
                if (response.statusCode() == 409) {
                    log.debug(
                            "UI callback duplicate ignored (idempotent retry): url={}, sequenceNumber={}",
                            eventPostUrl,
                            payload.getSequenceNumber());
                }
                return;
            }
            throw new KernelContextException(
                    "UI callback failed with status " + response.statusCode() + " for " + eventPostUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KernelContextException("failed to deliver UI callback to " + eventPostUrl, e);
        } catch (IOException e) {
            throw new KernelContextException("failed to deliver UI callback to " + eventPostUrl, e);
        }
    }

    /**
     * Posts the resolved workflow return message to POST /api/runs/{runId}/events.
     * No-op when no callback URL is present.
     */
    public static void reportWorkflowResult(
            KernelRuntimeContext context,
            String returnVariableName,
            Object returnVariableValue,
            String message,
            boolean usedAdminFallback) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(message, "message");

        if (resolveCallbackUrl(context.getInput()) == null) {
            return;
        }

        Context inputContext = context.getInput().getContext();
        String runId = inputContext != null ? inputContext.getRunId() : null;
        if (runId == null || runId.isBlank()) {
            throw new KernelContextException("runId is required to deliver workflow result callback");
        }

        String eventPostUrl = resolveEventPostUrl(context.getInput(), runId);
        String correlationId = inputContext.getCorrelationId();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", "WORKFLOW_RESULT");
        output.put("queue", context.getQueue());
        output.put("response", message);
        if (returnVariableName != null) {
            output.put("returnVariable", returnVariableName);
        }
        if (returnVariableValue != null) {
            output.put("returnValue", returnVariableValue);
        }
        output.put("variables", context.getVariableMap());
        output.put("usedAdminFallback", usedAdminFallback);

        RunEventCallbackPayload payload = new RunEventCallbackPayload(
                WORKFLOW_RESULT_SEQUENCE,
                EVENT_VERSION,
                "NODE_COMPLETED",
                correlationId,
                KERNEL_NODE_ID,
                "root",
                "SYSTEM",
                "COMPLETED",
                output,
                Map.of("phase", "kernel-result"));

        log.info(
                "Sending workflow result callback: runId={}, url={}, returnVariable={}, returnValue={}, message={}",
                runId,
                eventPostUrl,
                returnVariableName,
                formatLogValue(returnVariableValue),
                message);

        postPayload(eventPostUrl, payload);
    }

    static boolean isSuccessfulCallbackStatus(int statusCode) {
        return (statusCode >= 200 && statusCode < 300) || statusCode == 409;
    }

    private static String formatLogValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.length() <= 120) {
            return text;
        }
        return text.substring(0, 120) + "...";
    }

    static String resolveCallbackUrl(WorkflowInput input) {
        Execution execution = input.getExecution();
        if (execution != null && execution.getCallbackUrl() != null && !execution.getCallbackUrl().isBlank()) {
            return execution.getCallbackUrl();
        }
        Context context = input.getContext();
        if (context != null && context.getCallbackBaseUrl() != null && !context.getCallbackBaseUrl().isBlank()) {
            return context.getCallbackBaseUrl();
        }
        return null;
    }

    static String resolveEventPostUrl(WorkflowInput input, String runId) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(runId, "runId");

        Execution execution = input.getExecution();
        if (execution != null && execution.getCallbackUrl() != null && !execution.getCallbackUrl().isBlank()) {
            return execution.getCallbackUrl();
        }

        String callbackBaseUrl = resolveCallbackUrl(input);
        if (callbackBaseUrl == null || callbackBaseUrl.isBlank()) {
            return null;
        }

        String base = callbackBaseUrl.replaceAll("/+$", "");
        return base + "/api/runs/" + runId + "/events";
    }
}
