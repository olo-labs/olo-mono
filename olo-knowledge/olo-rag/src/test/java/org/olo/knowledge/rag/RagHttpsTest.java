// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.*;

import io.ololabs.toolgate.contracts.*;
import io.ololabs.toolgate.contracts.v2.RequestMode;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.*;

import javax.net.ssl.*;

/**
 * Real TLS sockets + separate application JVM + PostgreSQL. Fake Control supplies signed
 * permits/epoch/nonce checks.
 */
class RagHttpsTest {
    private final String tenant = UUID.randomUUID().toString(),
            conversation = UUID.randomUUID().toString();
    private KeyPair signing;
    private String executorKey;
    private long epoch = 1;
    private final Set<String> consumed = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    @Test
    void listenerEnforcesTlsPermitsExactBodiesOwnershipReplayAndRevocation() throws Exception {
        Assumptions.assumeTrue(
                System.getenv("KNOWLEDGE_PKI") != null && System.getenv("KNOWLEDGE_QDRANT") != null,
                "Run --with-tls for application TLS qualification");
        var folder = Path.of(System.getenv("KNOWLEDGE_PKI"));
        char[] password = System.getenv("KNOWLEDGE_PKI_PASSWORD").toCharArray();
        var pair = KeyPairGenerator.getInstance("RSA");
        pair.initialize(2048);
        signing = pair.generateKeyPair();
        var executorStore = store(folder.resolve("executor.p12"), password);
        executorKey =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                executorStore
                                                        .getCertificate("identity")
                                                        .getPublicKey()
                                                        .getEncoded()));
        var serverTls = tls(folder.resolve("server.p12"), folder.resolve("trust.p12"), password);
        var control = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 8);
        control.setHttpsConfigurator(new HttpsConfigurator(serverTls));
        control.createContext(
                "/api/control/v1/access/service-permits/olo-rag/",
                e -> {
                    try (e) {
                        if (!"Bearer fixture-service"
                                        .equals(e.getRequestHeaders().getFirst("Authorization"))
                                || !tenant.equals(
                                        e.getRequestHeaders().getFirst("Toolgate-Tenant-ID"))) {
                            e.sendResponseHeaders(403, -1);
                            return;
                        }
                        String path = e.getRequestURI().getPath();
                        Object response;
                        if (path.endsWith("epoch"))
                            response =
                                    Map.of(
                                            "tenantId",
                                            tenant,
                                            "authorizationEpoch",
                                            epoch,
                                            "observedAtUnixMs",
                                            System.currentTimeMillis());
                        else if (path.endsWith("jwks")) {
                            var rsa = (RSAPublicKey) signing.getPublic();
                            response =
                                    Map.of(
                                            "keys",
                                            List.of(
                                                    Map.of(
                                                            "kid",
                                                            "fixture-effect",
                                                            "kty",
                                                            "RSA",
                                                            "alg",
                                                            "RS256",
                                                            "use",
                                                            "sig",
                                                            "n",
                                                            unsigned(rsa.getModulus()),
                                                            "e",
                                                            unsigned(rsa.getPublicExponent()))));
                        } else {
                            var request = Json.MAPPER.readTree(e.getRequestBody());
                            String token = request.path("permit").path("jws").asText();
                            var claims =
                                    Json.MAPPER.readTree(
                                            Base64.getUrlDecoder().decode(token.split("\\.")[1]));
                            response =
                                    Map.of(
                                            "authorized",
                                            claims.path("authorizationEpoch").asLong() == epoch
                                                    && consumed.add(claims.path("nonce").asText()));
                        }
                        byte[] bytes = Json.MAPPER.writeValueAsBytes(response);
                        e.getResponseHeaders().set("Content-Type", "application/json");
                        e.sendResponseHeaders(200, bytes.length);
                        e.getResponseBody().write(bytes);
                    }
                });
        control.start();
        var db = PostgresFixture.create("https_rag", true);
        int port;
        try (var socket = new java.net.ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Path credential = folder.resolve("service.token");
        Files.writeString(credential, "fixture-service");
        var builder =
                new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("knowledge.rag.classpath"),
                        RagMain.class.getName());
        var env = builder.environment();
        env.putAll(
                Map.of(
                        "TENANT_ID",
                        tenant,
                        "PORT",
                        Integer.toString(port),
                        "DATABASE_URL",
                        PostgresFixture.url(db),
                        "DATABASE_USER",
                        "knowledge_test",
                        "DATABASE_PASSWORD",
                        System.getenv("KNOWLEDGE_TEST_PASSWORD")));
        env.putAll(
                Map.of(
                        "TLS_KEYSTORE",
                        folder.resolve("server.p12").toString(),
                        "TLS_TRUSTSTORE",
                        folder.resolve("trust.p12").toString(),
                        "TLS_STORE_PASSWORD",
                        new String(password),
                        "CONTROL_ORIGIN",
                        "https://localhost:" + control.getAddress().getPort(),
                        "CONTROL_ISSUER",
                        "fixture-control",
                        "CONTROL_CREDENTIAL_FILE",
                        credential.toString()));
        URI qdrant = URI.create(System.getenv("KNOWLEDGE_QDRANT")),
                ollama = URI.create(System.getenv("KNOWLEDGE_OLLAMA"));
        var model =
                new OllamaEmbeddings(
                        ollama, "all-minilm:latest", System.getenv("KNOWLEDGE_MODEL_DIGEST"));
        String collection = "tls_" + UUID.randomUUID().toString().replace("-", "");
        new JsonHttp(qdrant, null)
                .send(
                        "PUT",
                        "/collections/" + collection,
                        Map.of(
                                "vectors",
                                Map.of(
                                        "size",
                                        model.embed(List.of("dimension probe")).getFirst().size(),
                                        "distance",
                                        "Cosine")),
                        65536);
        env.putAll(
                Map.of(
                        "QDRANT_ORIGIN",
                        qdrant.toString(),
                        "QDRANT_API_KEY",
                        "fixture-key",
                        "QDRANT_COLLECTION",
                        collection,
                        "EMBEDDING_ORIGIN",
                        ollama.toString(),
                        "EMBEDDING_MODEL",
                        "all-minilm:latest",
                        "EMBEDDING_DIGEST",
                        System.getenv("KNOWLEDGE_MODEL_DIGEST")));
        var log = folder.resolve("rag.log");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        Process app = builder.start();
        try {
            var http =
                    HttpClient.newBuilder()
                            .sslContext(
                                    tls(
                                            folder.resolve("executor.p12"),
                                            folder.resolve("trust.p12"),
                                            password))
                            .connectTimeout(Duration.ofSeconds(2))
                            .build();
            URI endpoint = URI.create("https://localhost:" + port + "/v1/");
            boolean ready = false;
            for (int i = 0; i < 50; i++) {
                try {
                    ready =
                            http.send(
                                                    HttpRequest.newBuilder(
                                                                    endpoint.resolve("rag.query"))
                                                            .timeout(Duration.ofSeconds(1))
                                                            .GET()
                                                            .build(),
                                                    HttpResponse.BodyHandlers.discarding())
                                            .statusCode()
                                    == 405;
                    if (ready) break;
                } catch (java.io.IOException ignored) {
                }
                Thread.sleep(100);
            }
            assertTrue(ready, () -> "RAG process did not start: " + safeLog(log));
            byte[] append =
                    body(
                            Map.of(
                                    "create",
                                    true,
                                    "docId",
                                    UUID.randomUUID().toString(),
                                    "content",
                                    "private TLS fixture",
                                    "mediaType",
                                    "text/plain",
                                    "expectedVersion",
                                    0));
            String write =
                    permit("alice", "rag.ingest", append, tenant, System.currentTimeMillis());
            assertEquals(200, send(http, endpoint, "rag.ingest", append, write).statusCode());
            assertEquals(1, consumed.size());
            byte[] receipt = body(Map.of("type", "receipt", "invocationId", "inv-fixture"));
            var committed =
                    send(
                            http,
                            endpoint,
                            "rag.query",
                            receipt,
                            permit(
                                    "alice",
                                    "rag.query",
                                    receipt,
                                    tenant,
                                    System.currentTimeMillis()));
            assertEquals(200, committed.statusCode());
            assertEquals(
                    "COMMITTED", Json.MAPPER.readTree(committed.body()).path("state").asText());

            assertEquals(
                    403,
                    send(http, endpoint, "rag.ingest", append, write).statusCode(),
                    "A consumed write permit cannot replay");
            byte[] read = body(Map.of("query", "private fixture"));
            assertEquals(
                    403,
                    send(http, endpoint, "rag.query", read, null).statusCode(),
                    "A TLS peer alone grants nothing");
            assertEquals(
                    403,
                    send(
                                    http,
                                    endpoint,
                                    "rag.query",
                                    read,
                                    permit(
                                            "bob",
                                            "rag.query",
                                            read,
                                            tenant,
                                            System.currentTimeMillis()))
                            .statusCode(),
                    "Valid permit plus another user's resource still denied");
            assertEquals(
                    403,
                    send(
                                    http,
                                    endpoint,
                                    "rag.query",
                                    read,
                                    permit(
                                            "alice",
                                            "rag.query",
                                            read,
                                            UUID.randomUUID().toString(),
                                            System.currentTimeMillis()))
                            .statusCode());
            String valid = permit("alice", "rag.query", read, tenant, System.currentTimeMillis());
            assertEquals(200, send(http, endpoint, "rag.query", read, valid).statusCode());
            assertEquals(
                    403,
                    send(http, endpoint, "rag.query", body(Map.of("budget", 1)), valid)
                            .statusCode(),
                    "Exact-body binding prevents argument substitution");
            assertEquals(
                    403,
                    send(
                                    http,
                                    endpoint,
                                    "rag.query",
                                    read,
                                    permit(
                                            "alice",
                                            "rag.query",
                                            read,
                                            tenant,
                                            System.currentTimeMillis() - 16000))
                            .statusCode());
            var anonymous =
                    HttpClient.newBuilder()
                            .sslContext(tls(null, folder.resolve("trust.p12"), password))
                            .build();
            assertThrows(
                    java.io.IOException.class,
                    () -> send(anonymous, endpoint, "rag.query", read, valid),
                    "The listener requires a real client certificate");
            epoch = 2;
            long began = System.nanoTime();
            boolean denied = false;
            while (System.nanoTime() - began < Duration.ofSeconds(8).toNanos()) {
                if (send(http, endpoint, "rag.query", read, valid).statusCode() == 403) {
                    denied = true;
                    break;
                }
                Thread.sleep(100);
            }
            assertTrue(
                    denied,
                    "Revocation must become effective within the bounded Control refresh interval");
            assertTrue(System.nanoTime() - began < Duration.ofSeconds(10).toNanos());
        } finally {
            app.destroy();
            if (!app.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
                app.destroyForcibly().waitFor();
            control.stop(0);
            Arrays.fill(password, '\0');
        }
    }

    private byte[] body(Object request) throws Exception {
        return Json.MAPPER.writeValueAsBytes(
                Map.of("kbId", conversation, "request", Json.encode(request)));
    }

    private HttpResponse<String> send(
            HttpClient client, URI base, String operation, byte[] body, String permit)
            throws Exception {
        var request =
                HttpRequest.newBuilder(base.resolve(operation))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (permit != null) request.header("Toolgate-Service-Permit", permit);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String permit(String subject, String operation, byte[] body, String tid, long issued)
            throws Exception {
        var claims =
                new ServicePermitClaims(
                        "fixture-control",
                        ServiceAudience.olo_rag,
                        tid,
                        subject,
                        null,
                        RequestMode.HUMAN,
                        operation,
                        List.of(
                                new ResourceDescriptor(
                                        ResourceKind.CUSTOM,
                                        "olo://" + tid + "/kb/" + conversation)),
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)),
                        executorKey,
                        "inv-fixture",
                        UUID.randomUUID().toString(),
                        1L,
                        issued,
                        issued + 15000,
                        "execution-fixture");
        String message =
                B64.encodeToString(
                                Json.MAPPER.writeValueAsBytes(
                                        Map.of(
                                                "kid",
                                                "fixture-effect",
                                                "typ",
                                                "olo-service-permit+jws",
                                                "alg",
                                                "RS256")))
                        + "."
                        + B64.encodeToString(Json.MAPPER.writeValueAsBytes(claims));
        var signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(signing.getPrivate());
        signature.update(message.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return message + "." + B64.encodeToString(signature.sign());
    }

    private static String unsigned(java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        return B64.encodeToString(
                bytes[0] == 0 ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes);
    }

    private static KeyStore store(Path path, char[] password) throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(path)) {
            store.load(in, password);
        }
        return store;
    }

    private static SSLContext tls(Path identity, Path trust, char[] password) throws Exception {
        var tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tm.init(store(trust, password));
        KeyManager[] keys = null;
        if (identity != null) {
            var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(store(identity, password), password);
            keys = km.getKeyManagers();
        }
        var tls = SSLContext.getInstance("TLSv1.3");
        tls.init(keys, tm.getTrustManagers(), null);
        return tls;
    }

    private static String safeLog(Path path) {
        try {
            return Files.readString(path).substring(0, Math.min(2000, (int) Files.size(path)));
        } catch (Exception e) {
            return "Log unavailable";
        }
    }
}
