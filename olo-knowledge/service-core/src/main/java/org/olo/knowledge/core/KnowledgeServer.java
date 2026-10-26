// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.*;

import io.ololabs.toolgate.contracts.*;
import io.ololabs.toolgate.service.*;

import java.net.*;
import java.net.http.HttpClient;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

/**
 * Dedicated HTTPS listener: client certificates are mandatory; no proxy identity headers are
 * accepted.
 */
public final class KnowledgeServer {
    @FunctionalInterface
    public interface Handler {
        Object handle(ServiceRequest request, JsonNode body) throws Exception;
    }

    private KnowledgeServer() {}

    public static String env(String key) {
        var v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing " + key);
        return v;
    }

    public static Database database() {
        return new Database(env("DATABASE_URL"), env("DATABASE_USER"), env("DATABASE_PASSWORD"));
    }

    public static void start(ServiceAudience audience, Handler handler) throws Exception {
        String tenant = env("TENANT_ID");
        if (!tenant.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalArgumentException("Invalid tenant binding");
        var tls = tls();
        var http =
                HttpClient.newBuilder()
                        .sslContext(tls)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(2))
                        .build();
        var control =
                new ControlServiceAuthorization(
                        http,
                        URI.create(env("CONTROL_ORIGIN")),
                        env("CONTROL_ISSUER"),
                        tenant,
                        audience,
                        () -> {
                            try {
                                return Files.readString(Path.of(env("CONTROL_CREDENTIAL_FILE")))
                                        .strip();
                            } catch (java.io.IOException e) {
                                throw new SecurityException("Service credential unavailable");
                            }
                        },
                        Clock.systemUTC());
        var server =
                HttpsServer.create(
                        new InetSocketAddress(
                                Integer.parseInt(System.getenv().getOrDefault("PORT", "8443"))),
                        32);
        server.setHttpsConfigurator(
                new HttpsConfigurator(tls) {
                    @Override
                    public void configure(HttpsParameters p) {
                        var params = tls.getDefaultSSLParameters();
                        params.setNeedClientAuth(true);
                        params.setProtocols(new String[] {"TLSv1.3"});
                        p.setSSLParameters(params);
                    }
                });
        // Bounded concurrency and queue; exhausted callers retry through the durable dispatcher.
        server.setExecutor(
                new ThreadPoolExecutor(
                        4,
                        16,
                        30,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(64),
                        new ThreadPoolExecutor.AbortPolicy()));
        server.createContext(
                "/v1/",
                exchange -> {
                    try {
                        if (!exchange.getRequestMethod().equals("POST")
                                || exchange.getRequestURI().getRawQuery() != null) {
                            reply(exchange, 405, Map.of("error", "POST required"));
                            return;
                        }
                        String op = exchange.getRequestURI().getPath().substring(4);
                        String prefix = audience == ServiceAudience.olo_memory ? "memory." : "rag.";
                        if (!op.startsWith(prefix) || op.indexOf('/') >= 0)
                            throw new IllegalArgumentException("Invalid operation");
                        var bytes = exchange.getRequestBody().readNBytes(33_554_433);
                        if (bytes.length > 33_554_432) {
                            reply(exchange, 413, Map.of("error", "Body limit"));
                            return;
                        }
                        JsonNode body = Json.MAPPER.readTree(bytes);
                        if (body == null || !body.isObject())
                            throw new IllegalArgumentException("Object required");
                        // Root resource is derived from actual endpoint arguments, never from
                        // submitted permit claims.
                        String id =
                                Json.id(
                                        body,
                                        audience == ServiceAudience.olo_memory
                                                ? "conversationId"
                                                : "kbId");
                        String locator =
                                "olo://"
                                        + tenant
                                        + "/"
                                        + (audience == ServiceAudience.olo_memory
                                                ? "conversation/"
                                                : "kb/")
                                        + id;
                        var resources =
                                List.of(new ResourceDescriptor(ResourceKind.CUSTOM, locator));
                        String token =
                                exchange.getRequestHeaders().getFirst("Toolgate-Service-Permit");
                        if (token == null) throw Scope.denied();
                        var request =
                                new ServiceRequest(
                                        control,
                                        new ServicePermit(token),
                                        new ServicePermitVerifier.Request(
                                                tenant, op, resources, bytes),
                                        ((HttpsExchange) exchange).getSSLSession());
                        reply(exchange, 200, handler.handle(request, body));
                    } catch (Database.CommitUncertain uncertain) {
                        safeReply(exchange, 503, "OUTCOME_UNKNOWN: reconcile the storage receipt");
                    } catch (SecurityException denied) {
                        var origin = denied.getStackTrace();
                        String source =
                                origin.length == 0
                                        ? "unknown"
                                        : origin[0].getClassName()
                                                + "#"
                                                + origin[0].getMethodName();
                        String caller = origin.length < 2 ? "unknown" : origin[1].toString();
                        // Code locations only: never exception messages, request bodies, permits or
                        // credentials.
                        System.getLogger(KnowledgeServer.class.getName())
                                .log(
                                        System.Logger.Level.WARNING,
                                        "authorization_denied source="
                                                + source
                                                + " caller="
                                                + caller);
                        safeReply(exchange, 403, "Access denied");
                    } catch (IllegalArgumentException invalid) {
                        safeReply(exchange, 400, "Invalid request");
                    } catch (Exception failure) {
                        safeReply(exchange, 503, "Operation incomplete; reconcile before retrying");
                    } finally {
                        exchange.close();
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(5)));
        server.start();
    }

    private static void safeReply(HttpExchange e, int code, String message) {
        try {
            reply(e, code, Map.of("error", message));
        } catch (Exception ignored) {
        }
    }

    private static void reply(HttpExchange e, int code, Object value) throws java.io.IOException {
        byte[] bytes = Json.MAPPER.writeValueAsBytes(value);
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        if (bytes.length > 1_048_576)
            throw new IllegalArgumentException("Response budget exceeded");
        e.sendResponseHeaders(code, bytes.length);
        e.getResponseBody().write(bytes);
    }

    private static SSLContext tls() throws Exception {
        char[] password = env("TLS_STORE_PASSWORD").toCharArray();
        try {
            var keys = KeyStore.getInstance("PKCS12");
            try (var in = Files.newInputStream(Path.of(env("TLS_KEYSTORE")))) {
                keys.load(in, password);
            }
            var trust = KeyStore.getInstance("PKCS12");
            try (var in = Files.newInputStream(Path.of(env("TLS_TRUSTSTORE")))) {
                trust.load(in, password);
            }
            var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(keys, password);
            var tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tm.init(trust);
            var context = SSLContext.getInstance("TLSv1.3");
            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            return context;
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
