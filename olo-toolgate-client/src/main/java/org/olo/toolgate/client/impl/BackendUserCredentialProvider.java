/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.olo.toolgate.client.ToolGateException;
import org.olo.toolgate.client.UserBinding;
import org.olo.toolgate.client.UserCredentialProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gets ToolGate user credentials from the OLO backend ({@code POST /api/internal/toolgate/credentials}), signing each
 * request as a worker. A credential is reused until 30 seconds before it expires and never written anywhere; a refusal
 * (session ended, user not a member or not enabled) is never retried with another identity.
 */
public final class BackendUserCredentialProvider implements UserCredentialProvider {

    /** Signs a worker request, for example with the worker callback key. */
    @FunctionalInterface
    public interface RequestSigner {
        HttpRequest.Builder sign(HttpRequest.Builder request, URI uri, byte[] body);
    }

    static final String PATH = "/api/internal/toolgate/credentials";
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);
    private static final int MAX_CACHED = 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final URI endpoint;
    private final RequestSigner signer;
    private final HttpClient http;
    private final Duration timeout;
    private final Clock clock;
    private final Map<UserBinding, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(String token, Instant refreshAt) {
    }

    public BackendUserCredentialProvider(URI backendBaseUrl, RequestSigner signer, Duration timeout) {
        this(backendBaseUrl, signer, HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build(), timeout, Clock.systemUTC());
    }

    BackendUserCredentialProvider(URI backendBaseUrl, RequestSigner signer, HttpClient http, Duration timeout, Clock clock) {
        this.endpoint = URI.create(backendBaseUrl.toString().replaceAll("/+$", "") + PATH);
        this.signer = Objects.requireNonNull(signer, "signer");
        this.http = Objects.requireNonNull(http, "http");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String credential(UserBinding binding) {
        Instant now = clock.instant();
        Cached cached = cache.get(binding);
        if (cached != null && now.isBefore(cached.refreshAt())) {
            return cached.token();
        }
        cache.remove(binding);
        Cached fresh = fetch(binding, now);
        if (cache.size() >= MAX_CACHED) {
            cache.clear();
        }
        cache.put(binding, fresh);
        return fresh.token();
    }

    private Cached fetch(UserBinding binding, Instant now) {
        ObjectNode body = JSON.createObjectNode()
                .put("tenantId", binding.tenantId())
                .put("userId", binding.userId())
                .put("issuer", binding.issuer())
                .put("sessionId", binding.sessionId())
                .put("sessionEpoch", binding.sessionEpoch());
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpRequest request = signer.sign(HttpRequest.newBuilder(endpoint)
                        .timeout(timeout)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)), endpoint, bytes)
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "OLO backend unreachable for a user credential", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE, "interrupted while getting a user credential", e);
        }
        JsonNode reply = parse(response.body());
        if (response.statusCode() == 200 && reply.path("accessToken").isTextual() && reply.path("expiresIn").canConvertToLong()) {
            Duration lifetime = Duration.ofSeconds(reply.get("expiresIn").asLong());
            Duration reuse = lifetime.compareTo(REFRESH_MARGIN.multipliedBy(2)) > 0 ? lifetime.minus(REFRESH_MARGIN) : lifetime.dividedBy(2);
            return new Cached(reply.get("accessToken").asText(), now.plus(reuse));
        }
        String code = reply.path("error").asText("refused");
        if (response.statusCode() == 403 || response.statusCode() == 401) {
            throw new ToolGateException(ToolGateException.Kind.FORBIDDEN,
                    "OLO backend refused a ToolGate credential for the run's user: " + code);
        }
        throw new ToolGateException(ToolGateException.Kind.UNAVAILABLE,
                "OLO backend did not issue a ToolGate credential (HTTP " + response.statusCode() + ", " + code + ")");
    }

    private static JsonNode parse(String body) {
        try {
            JsonNode node = body == null || body.isBlank() ? null : JSON.readTree(body);
            return node == null ? JSON.createObjectNode() : node;
        } catch (IOException e) {
            return JSON.createObjectNode();
        }
    }
}
