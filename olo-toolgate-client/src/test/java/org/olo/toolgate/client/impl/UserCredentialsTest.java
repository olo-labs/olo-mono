/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.olo.toolgate.client.FakeGateway;
import org.olo.toolgate.client.ToolGateException;
import org.olo.toolgate.client.ToolGateSettings;
import org.olo.toolgate.client.UserBinding;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** M3.3: user credentials come from the backend, signed as a worker, and are what a user-scoped client sends. */
class UserCredentialsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UserBinding ALICE = new UserBinding("t1", "user-42", "https://idp.example", "sid-1", 3);

    private final List<JsonNode> bodies = new CopyOnWriteArrayList<>();
    private final List<String> signatures = new CopyOnWriteArrayList<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicInteger issued = new AtomicInteger();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-24T10:00:00Z"));
    private HttpServer backend;
    private BackendUserCredentialProvider provider;

    @BeforeEach
    void start() throws Exception {
        backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backend.createContext(BackendUserCredentialProvider.PATH, exchange -> {
            bodies.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
            signatures.add(exchange.getRequestHeaders().getFirst("X-Test-Signature"));
            byte[] body = (status.get() == 200
                    ? "{\"accessToken\":\"user-token-" + issued.incrementAndGet() + "\",\"tokenType\":\"Bearer\",\"expiresIn\":120}"
                    : "{\"error\":\"session_ended\",\"message\":\"no\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        backend.start();
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        provider = new BackendUserCredentialProvider(URI.create("http://127.0.0.1:" + backend.getAddress().getPort() + "/"),
                (request, uri, body) -> request.header("X-Test-Signature", uri.getPath() + ":" + body.length),
                HttpClient.newHttpClient(), Duration.ofSeconds(5), clock);
    }

    @AfterEach
    void stop() {
        backend.stop(0);
    }

    @Test
    void signedRequestCarriesTheBindingAndTheCredentialIsReusedUntilNearExpiry() {
        assertThat(provider.credential(ALICE)).isEqualTo("user-token-1");
        JsonNode body = bodies.get(0);
        assertThat(body.get("tenantId").asText()).isEqualTo("t1");
        assertThat(body.get("userId").asText()).isEqualTo("user-42");
        assertThat(body.get("issuer").asText()).isEqualTo("https://idp.example");
        assertThat(body.get("sessionId").asText()).isEqualTo("sid-1");
        assertThat(body.get("sessionEpoch").asLong()).isEqualTo(3);
        assertThat(signatures.get(0)).startsWith(BackendUserCredentialProvider.PATH + ":");

        now.set(now.get().plusSeconds(60));
        assertThat(provider.credential(ALICE)).isEqualTo("user-token-1");
        now.set(now.get().plusSeconds(31));
        assertThat(provider.credential(ALICE)).isEqualTo("user-token-2");
        assertThat(bodies).hasSize(2);
    }

    @Test
    void aRefusedUserIsForbiddenAndNothingIsCached() {
        status.set(403);
        assertThatThrownBy(() -> provider.credential(ALICE)).isInstanceOf(ToolGateException.class)
                .extracting(e -> ((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.FORBIDDEN);
        status.set(200);
        assertThat(provider.credential(ALICE)).isEqualTo("user-token-1");
    }

    @Test
    void aUserScopedClientSendsOnlyTheUsersCredential() throws Exception {
        try (FakeGateway gateway = new FakeGateway()) {
            gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"x\",\"result\":{\"tools\":[]}}"));
            var settings = new ToolGateSettings(gateway.mcpUrl(), null, null, Duration.ofSeconds(5));
            var agentTokens = new FileAndEnvCredentialResolver(null, Map.of("OLO_TOOLGATE_TOKEN_MEMORY_AGENT", "agent-token"));

            new McpToolGateClient(settings, agentTokens, provider).forUser(ALICE).listTools("memory-agent");
            assertThat(gateway.received().get(0).header("Authorization")).isEqualTo("Bearer user-token-1");

            assertThatThrownBy(() -> new McpToolGateClient(settings, agentTokens, (org.olo.toolgate.client.UserCredentialProvider) null).forUser(ALICE))
                    .isInstanceOf(ToolGateException.class)
                    .extracting(e -> ((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.CREDENTIAL_MISSING);
        }
    }
}
