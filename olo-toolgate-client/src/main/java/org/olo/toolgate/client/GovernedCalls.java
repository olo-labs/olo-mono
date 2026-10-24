// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Paths A/C. No catalog selection, model decision, delegated actor, or stored bearer credential.
 */
public final class GovernedCalls {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ToolGateClient client;

    public GovernedCalls(ToolGateClient client) {
        this.client = Objects.requireNonNull(client);
    }

    public JsonNode user(
            UserBinding identity,
            String operationId,
            String operation,
            String resource,
            Map<String, Object> request) {
        if (identity == null || identity.userId() == null)
            throw new SecurityException("Verified end-user identity required");
        return invoke(
                client.forUser(identity), "platform", operationId, operation, resource, request);
    }

    /**
     * A separately configured service credential resolver must restrict this principal to its
     * resource grants.
     */
    public JsonNode service(
            String tenant,
            String servicePrincipal,
            String operationId,
            String operation,
            String resource,
            Map<String, Object> request) {
        if (!operation.equals("rag.update"))
            throw new SecurityException("Background principal may only index/reindex");
        return invoke(
                client.forService(tenant, servicePrincipal),
                servicePrincipal,
                operationId,
                operation,
                resource,
                request);
    }

    private JsonNode invoke(
            ToolGateClient caller,
            String route,
            String operationId,
            String op,
            String resource,
            Map<String, Object> request) {
        if (!Set.of(
                        "memory.read",
                        "memory.append",
                        "memory.delete",
                        "memory.share",
                        "memory.export",
                        "rag.query",
                        "rag.ingest",
                        "rag.update",
                        "rag.delete",
                        "rag.share")
                .contains(op)) throw new IllegalArgumentException("Unknown platform operation");
        UUID.fromString(resource);
        try {
            var args =
                    JSON.createObjectNode()
                            .put(op.startsWith("memory.") ? "conversationId" : "kbId", resource)
                            .put("request", JSON.writeValueAsString(request));
            // Replaying the same logical operation keeps the caller-scoped M5 invocation, including
            // ASK.
            var result =
                    caller.callTool(
                            route, stableId(operationId), "olo." + op.replace('.', '/'), args);
            if (result.outcomeUnknown())
                throw new ReconciliationRequired(operationId, result.invocationId());
            if (result.pendingApproval()
                    || Set.of("QUEUED", "ADMITTED", "EXECUTING")
                            .contains(Objects.toString(result.state(), "")))
                throw new Pending(operationId);
            if (result.error() || result.ended())
                throw new SecurityException("Governed platform operation refused");
            JsonNode content = result.structuredContent();
            if (content != null && content.has("json"))
                return JSON.readTree(content.path("json").asText());
            // MCP runtimes may return a typed result as a text content block.
            if (content == null) content = JSON.readTree(result.text());
            if (content != null && content.has("json"))
                return JSON.readTree(content.path("json").asText());
            if (content == null || !content.isObject())
                throw new IllegalStateException("Invalid platform result");
            return content;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Invalid platform JSON", e);
        }
    }

    public static String stableId(String value) {
        return "platform-" + UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public static final class Pending extends RuntimeException {
        public Pending(String id) {
            super("Governed call pending: " + id);
        }
    }

    public static final class ReconciliationRequired extends RuntimeException {
        private final String invocationId;

        public ReconciliationRequired(String id) {
            this(id, null);
        }

        public ReconciliationRequired(String id, String invocationId) {
            super("Reconciliation required: " + id);
            this.invocationId = invocationId;
        }

        public String invocationId() {
            return invocationId;
        }
    }
}
