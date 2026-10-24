/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.olo.toolgate.client.ToolGateCallResult;
import org.olo.toolgate.client.ToolGateClient;
import org.olo.toolgate.client.ToolGateCredentialResolver;
import org.olo.toolgate.client.ToolGateException;
import org.olo.toolgate.client.ToolGateSettings;
import org.olo.toolgate.client.ToolGateTool;
import org.olo.toolgate.client.UserBinding;
import org.olo.toolgate.client.UserCredentialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link ToolGateClient} over the Gateway's stateless MCP 2026-07-28 endpoint ({@code POST /mcp}).
 */
public final class McpToolGateClient implements ToolGateClient {

    public static final String PROTOCOL_VERSION = "2026-07-28";

    private static final Logger log = LoggerFactory.getLogger(McpToolGateClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final int MAX_ERROR_DETAIL = 300;
    /** The Gateway header for an agent's on-behalf-of workload token (ToolGate M3.10). */
    public static final String ACTOR_CREDENTIAL_HEADER = "X-ToolGate-Actor-Credential";

    private final ToolGateSettings settings;
    private final ToolGateCredentialResolver credentials;
    private final HttpClient httpClient;
    private final UserCredentialProvider users;
    /** Agent workload tokens sent as the actor beside a user credential (path B, M3.10); null otherwise. */
    private final ToolGateCredentialResolver actors;

    public McpToolGateClient(ToolGateSettings settings, ToolGateCredentialResolver credentials) {
        this(settings, credentials, buildHttpClient(settings));
    }

    /** Agent workload tokens, plus {@code users} for {@link #forUser}; {@code users} may be null. */
    public McpToolGateClient(ToolGateSettings settings, ToolGateCredentialResolver credentials, UserCredentialProvider users) {
        this(settings, credentials, buildHttpClient(settings), users);
    }

    McpToolGateClient(ToolGateSettings settings, ToolGateCredentialResolver credentials, HttpClient httpClient) {
        this(settings, credentials, httpClient, null);
    }

    McpToolGateClient(ToolGateSettings settings, ToolGateCredentialResolver credentials, HttpClient httpClient,
                      UserCredentialProvider users) {
        this(settings, credentials, httpClient, users, null);
    }

    private McpToolGateClient(ToolGateSettings settings, ToolGateCredentialResolver credentials, HttpClient httpClient,
                              UserCredentialProvider users, ToolGateCredentialResolver actors) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.users = users;
        this.actors = actors;
    }

    @Override
    public List<ToolGateTool> listTools(String agentName) {
        String token = credentials.resolveToken(agentName);
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        Reply reply = send(token, actor(agentName), "tools/list", null, "olo-list-" + UUID.randomUUID(), params);
        if (reply.error() != null) {
            throw failure(reply, "tools/list for agent '" + agentName + "'");
        }
        JsonNode tools = reply.result().path("tools");
        if (!tools.isArray()) {
            throw new ToolGateException(ToolGateException.Kind.PROTOCOL, "tools/list result has no tools array");
        }
        List<ToolGateTool> catalog = new ArrayList<>();
        for (JsonNode tool : tools) {
            String name = tool.path("name").asText("");
            if (name.isBlank()) {
                continue;
            }
            catalog.add(new ToolGateTool(name, tool.path("description").asText(""), tool.get("inputSchema")));
        }
        log.debug("ToolGate catalog for agent {}: {}", agentName, catalog.stream().map(ToolGateTool::name).toList());
        return List.copyOf(catalog);
    }

    @Override
    public ToolGateCallResult callTool(String agentName, String requestId, String toolName, JsonNode arguments) {
        Objects.requireNonNull(toolName, "toolName");
        if (requestId == null || requestId.isBlank() || requestId.length() > MAX_REQUEST_ID_LENGTH) {
            throw new IllegalArgumentException("requestId must be 1-" + MAX_REQUEST_ID_LENGTH + " characters");
        }
        String token = credentials.resolveToken(agentName);
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("name", toolName);
        params.set("arguments", arguments != null && arguments.isObject()
                ? arguments
                : JsonNodeFactory.instance.objectNode());

        Reply reply = send(token, actor(agentName), "tools/call", toolName, requestId, params);
        if (reply.error() != null) {
            // The tool is not in this agent's catalog: let the model see it instead of failing the run.
            if (reply.status() == 403 && reply.error().path("code").asInt() == -32602) {
                return ToolGateCallResult.failure("Tool '" + toolName + "' is not available to agent '" + agentName + "'.");
            }
            // The executor ran or refused the call: a tool-level failure the model can react to.
            if (reply.status() == 200 && reply.error().path("code").asInt() == -32000) {
                JsonNode data = reply.error().path("data");
                if (ToolGateCallResult.STATE_OUTCOME_UNKNOWN.equals(data.path("outcome").asText())
                        || ToolGateCallResult.STATE_OUTCOME_UNKNOWN.equals(data.path("error").path("code").asText())) {
                    ObjectNode unknown = MAPPER.createObjectNode();
                    unknown.put("state", ToolGateCallResult.STATE_OUTCOME_UNKNOWN);
                    String invocationId = textOrNull(data.get("requestId"));
                    if (invocationId != null) unknown.put("invocationId", invocationId);
                    if (data.has("reconciliation")) unknown.set("reconciliation", data.get("reconciliation"));
                    return new ToolGateCallResult(true, "Tool outcome is unknown; reconciliation is required.",
                            unknown, ToolGateCallResult.STATE_OUTCOME_UNKNOWN, invocationId);
                }
                return ToolGateCallResult.failure("Tool '" + toolName + "' failed: " + describe(reply.error()));
            }
            throw failure(reply, "tools/call " + toolName + " for agent '" + agentName + "'");
        }

        JsonNode result = reply.result();
        JsonNode structured = result.get("structuredContent");
        String state = structured != null ? textOrNull(structured.get("state")) : null;
        String invocationId = structured != null ? textOrNull(structured.get("invocationId")) : null;
        return new ToolGateCallResult(
                result.path("isError").asBoolean(false),
                joinText(result.path("content")),
                structured,
                state,
                invocationId);
    }

    @Override
    public ToolGateClient forUser(UserBinding binding) {
        Objects.requireNonNull(binding, "binding");
        if (users == null) {
            throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING, "ToolGate user credentials are not configured");
        }
        // Every token this view sends is the user's; the agent name only selects the catalog route.
        return new McpToolGateClient(settings, agentName -> users.credential(binding), httpClient, null, null);
    }

    @Override
    public ToolGateClient forUserViaAgent(UserBinding binding) {
        Objects.requireNonNull(binding, "binding");
        if (users == null) {
            throw new ToolGateException(ToolGateException.Kind.CREDENTIAL_MISSING, "ToolGate user credentials are not configured");
        }
        // The user credential is the bearer; the agent's on-behalf-of workload token travels beside it as the actor.
        return new McpToolGateClient(settings, agentName -> users.credential(binding), httpClient, null, credentials);
    }

    private String actor(String agentName) {
        return actors == null ? null : actors.resolveToken(agentName);
    }

    @Override
    public void probe() {
        ObjectNode body = frame("server/discover", "olo-probe", JsonNodeFactory.instance.objectNode());
        HttpRequest request = baseRequest("server/discover", null)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.debug("ToolGate probe {} answered HTTP {}", settings.mcpUrl(), response.statusCode());
        } catch (HttpTimeoutException e) {
            throw new ToolGateException(ToolGateException.Kind.TIMEOUT, "ToolGate Gateway did not answer: " + settings.mcpUrl(), e);
        } catch (IOException e) {
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "ToolGate Gateway unreachable: " + settings.mcpUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "interrupted while probing ToolGate", e);
        }
    }

    private Reply send(String token, String actor, String method, String toolName, String id, ObjectNode params) {
        ObjectNode body = frame(method, id, params);
        HttpRequest.Builder builder = baseRequest(method, toolName).header("Authorization", "Bearer " + token);
        if (actor != null) {
            builder.header(ACTOR_CREDENTIAL_HEADER, actor);
        }
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ToolGateException(ToolGateException.Kind.TIMEOUT,
                    "ToolGate " + method + " timed out after " + settings.timeout(), e);
        } catch (IOException e) {
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE,
                    "ToolGate Gateway unreachable at " + settings.mcpUrl() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "interrupted during ToolGate " + method, e);
        }

        int status = response.statusCode();
        JsonNode json = parseBody(response);
        if (json == null || !json.has("jsonrpc")) {
            // Plain HTTP ErrorEnvelope (authentication, limits, tenant checks) or an unexpected body.
            throw httpFailure(status, method, json);
        }
        JsonNode error = json.get("error");
        if (error != null && !error.isNull()) {
            return new Reply(status, null, error);
        }
        JsonNode result = json.get("result");
        if (result == null || !result.isObject() || status < 200 || status >= 300) {
            throw new ToolGateException(ToolGateException.Kind.PROTOCOL,
                    "ToolGate " + method + " returned HTTP " + status + " without a result");
        }
        return new Reply(status, result, null);
    }

    private HttpRequest.Builder baseRequest(String method, String toolName) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(settings.mcpUrl())
                .timeout(settings.timeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .header("Mcp-Method", method);
        if (toolName != null) {
            builder.header("Mcp-Name", headerSafeName(toolName));
        }
        return builder;
    }

    private static ObjectNode frame(String method, String id, ObjectNode params) {
        ObjectNode meta = params.putObject("_meta");
        meta.put("io.modelcontextprotocol/protocolVersion", PROTOCOL_VERSION);
        meta.putObject("io.modelcontextprotocol/clientCapabilities");
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("method", method);
        body.set("params", params);
        return body;
    }

    /** Non-ASCII names use the Gateway's {@code =?base64?...?=} form. */
    static String headerSafeName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x21 || c > 0x7e) {
                return "=?base64?" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)) + "?=";
            }
        }
        return name;
    }

    private static JsonNode parseBody(HttpResponse<String> response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return null;
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        String payload = contentType.startsWith("text/event-stream") ? lastSseData(body) : body;
        if (payload == null) {
            return null;
        }
        try {
            return MAPPER.readTree(payload);
        } catch (IOException e) {
            return null;
        }
    }

    /** The JSON-RPC response is the last SSE event's {@code data} (multi-line data joined with newlines). */
    static String lastSseData(String body) {
        String last = null;
        StringBuilder current = null;
        for (String line : body.split("\r?\n", -1)) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).startsWith(" ") ? line.substring(6) : line.substring(5);
                current = current == null ? new StringBuilder(data) : current.append('\n').append(data);
            } else if (line.isEmpty() && current != null) {
                last = current.toString();
                current = null;
            }
        }
        return current != null ? current.toString() : last;
    }

    private static ToolGateException failure(Reply reply, String what) {
        int status = reply.status();
        ToolGateException.Kind kind = kindForStatus(status);
        if (status == 200) {
            kind = ToolGateException.Kind.UNAVAILABLE;
        } else if (status == 400 || status == 404 || status == 406) {
            kind = ToolGateException.Kind.PROTOCOL;
        }
        return new ToolGateException(kind, "ToolGate " + what + " failed (HTTP " + status + "): " + describe(reply.error()));
    }

    private static ToolGateException httpFailure(int status, String method, JsonNode envelope) {
        String detail = envelope != null ? truncate(envelope.path("error").path("code").asText(envelope.toString())) : "";
        return new ToolGateException(kindForStatus(status),
                "ToolGate " + method + " rejected with HTTP " + status + (detail.isEmpty() ? "" : ": " + detail));
    }

    private static ToolGateException.Kind kindForStatus(int status) {
        return switch (status) {
            case 401 -> ToolGateException.Kind.UNAUTHORIZED;
            case 403 -> ToolGateException.Kind.FORBIDDEN;
            case 504 -> ToolGateException.Kind.TIMEOUT;
            case 429, 502, 503 -> ToolGateException.Kind.UNAVAILABLE;
            default -> status >= 500 ? ToolGateException.Kind.UNAVAILABLE : ToolGateException.Kind.PROTOCOL;
        };
    }

    private static String describe(JsonNode error) {
        String message = error.path("message").asText("error");
        String code = error.path("data").path("code").asText("");
        return truncate(code.isEmpty() ? message : message + " [" + code + "]");
    }

    private static String joinText(JsonNode content) {
        if (!content.isArray()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText()) && block.hasNonNull("text")) {
                parts.add(block.get("text").asText());
            }
        }
        return String.join("\n", parts);
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private static String truncate(String text) {
        return text.length() <= MAX_ERROR_DETAIL ? text : text.substring(0, MAX_ERROR_DETAIL) + "...";
    }

    private static HttpClient buildHttpClient(ToolGateSettings settings) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(settings.timeout())
                .followRedirects(HttpClient.Redirect.NEVER);
        if (settings.caCertificate() != null) {
            builder.sslContext(sslContextFor(settings.caCertificate()));
        }
        return builder.build();
    }

    private static SSLContext sslContextFor(Path caCertificate) {
        try (InputStream in = Files.newInputStream(caCertificate)) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            int index = 0;
            for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(in)) {
                trustStore.setCertificateEntry("toolgate-ca-" + index++, certificate);
            }
            if (index == 0) {
                throw new IllegalArgumentException("no certificates in ToolGate CA file " + caCertificate);
            }
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trust.getTrustManagers(), null);
            return context;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("cannot load ToolGate CA certificate " + caCertificate + ": " + e.getMessage(), e);
        }
    }

    private record Reply(int status, JsonNode result, JsonNode error) {
    }
}
