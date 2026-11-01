// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.memory;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.*;

import io.ololabs.toolgate.contracts.*;
import io.ololabs.toolgate.contracts.v2.RequestMode;

import org.junit.jupiter.api.*;
import org.olo.knowledge.core.*;
import org.olo.knowledge.testing.PostgresFixture;
import org.olo.knowledge.testing.ScriptedClaims;
import org.olo.knowledge.testing.FencingFixture;

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
class MemoryHttpsTest {
    private final String tenant = UUID.randomUUID().toString(),
            conversation = UUID.randomUUID().toString();
    private KeyPair signing;
    private int servicePort;
    private String executorKey;
    private volatile long epoch = 1;
    private final Set<String> consumed = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    @Test
    void listenerEnforcesTlsPermitsExactBodiesOwnershipReplayAndRevocation() throws Exception {
        Assumptions.assumeTrue(
                System.getenv("KNOWLEDGE_PKI") != null,
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
        var claimControl=new ScriptedClaims(consumed,()->epoch);
        control.createContext(
                "/api/control/v1/access/service-permits/olo-memory/",
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
                        } else if(path.contains("/claims/")){
                            var claim=claimControl.handle(path,Json.MAPPER.readTree(e.getRequestBody()));
                            if(claim.status()!=200){if(claim.status()!=0)e.sendResponseHeaders(claim.status(),-1);return;}
                            response=claim.body();
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
                                                    && request.path("httpRequestDigest").asText().equals(claims.path("httpRequestDigest").asText())
                                                    && consumed.add(claims.path("nonce").asText()));
                        }
                        byte[] bytes = Json.MAPPER.writeValueAsBytes(response);
                        e.getResponseHeaders().set("Content-Type", "application/json");
                        e.sendResponseHeaders(200, bytes.length);
                        e.getResponseBody().write(bytes);
                    }
                });
        control.start();
        var db = PostgresFixture.create("https_memory", false);
        int port;
        try (var socket = new java.net.ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        servicePort=port;
        Path transportFile=folder.resolve("transport-memory.json");
        Files.writeString(transportFile,Json.encode(Map.of(
            "schemaVersion",1,"audience","olo-memory","origin","https://localhost:"+port,
            "profileId","sp-fixture-memory","profileDigest","sha256:"+"a".repeat(64),
            "routes",Map.of("memory.read","read","memory.append","append","memory.delete","delete","memory.share","share","memory.export","export"))));
        Path credential = folder.resolve("service.token");
        Files.writeString(credential, "fixture-service");
        var builder =
                new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("knowledge.memory.classpath"),
                        MemoryMain.class.getName());
        var env = builder.environment();
        env.put("TOOLGATE_TRANSPORT_BINDINGS_FILE",transportFile.toString());
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
        var log = folder.resolve("memory.log");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        Process app = builder.start();
        FencingFixture.Replica replica=null;
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
                                                                    endpoint.resolve("memory.read"))
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
            assertTrue(ready, () -> "Memory process did not start: " + safeLog(log));
            replica=FencingFixture.replica(builder,http,"memory.read",folder.resolve("memory_message-replica.log"));
            URI peer=replica.endpoint();
            byte[] append =
                    body(
                            Map.of(
                                    "create",
                                    true,
                                    "messageId",
                                    UUID.randomUUID().toString(),
                                    "turnId",
                                    UUID.randomUUID().toString(),
                                    "role",
                                    "user",
                                    "content",
                                    "private TLS fixture",
                                    "dependencies",
                                    List.of()));
            String write =
                    permit("alice", "memory.append", append, tenant, System.currentTimeMillis());
            assertEquals(405,send(http,endpoint,"memory.append?recipient=changed",append,write).statusCode());
            assertEquals(0,consumed.size(),"Changed HTTP transport must not consume a write capability");
            var changedAccept=HttpRequest.newBuilder(endpoint.resolve("memory.append")).timeout(Duration.ofSeconds(8))
                .header("Content-Type","application/json").header("Accept","application/problem+json")
                .header("Toolgate-Service-Permit",write).POST(HttpRequest.BodyPublishers.ofByteArray(append)).build();
            assertEquals(403,http.send(changedAccept,HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(0,consumed.size(),"Changed semantic header must not consume a write capability");

            String legacy=permit("alice","memory.append",append,tenant,System.currentTimeMillis(),false);
            assertEquals(403,send(http,endpoint,"memory.append",append,legacy).statusCode());
            assertEquals(0,consumed.size(),"Unbound downgrade must not consume a write capability");
            claimControl.consumptionAvailable=false;
            String unavailable=permit("alice","memory.append",append,tenant,System.currentTimeMillis());
            assertEquals(403,send(http,endpoint,"memory.append",append,unavailable).statusCode());
            claimControl.consumptionAvailable=true;
            assertEquals(409,send(http,endpoint,"memory.append",append,unavailable).statusCode(),"An uncertain consumption must not create a second local owner");
            claimControl.forceOtherClaim=true;
            try{
                String other=permit("alice","memory.append",append,tenant,System.currentTimeMillis());
                assertEquals(403,send(http,endpoint,"memory.append",append,other).statusCode());
            }finally{claimControl.forceOtherClaim=false;}
            claimControl.corruptGeneration=true;
            try{
                String wrongGeneration=permit("alice","memory.append",append,tenant,System.currentTimeMillis());
                assertEquals(403,send(http,endpoint,"memory.append",append,wrongGeneration).statusCode());
            }finally{claimControl.corruptGeneration=false;}
            String paused=permit("alice","memory.append",append,tenant,System.currentTimeMillis());
            FencingFixture.pausedOwner(db,tenant,endpoint,paused,claimControl,(target,grant)->send(http,target,"memory.append",append,grant));
            claimControl.outcomesAvailable=false;
            int beforeWrite=consumed.size();
            String raceGrant=permit("alice","memory.append",append,tenant,System.currentTimeMillis());
            var race=FencingFixture.race(endpoint,peer,raceGrant,(target,grant)->send(http,target,"memory.append",append,grant));
            assertEquals(Set.of(200,409),new HashSet<>(List.of(race.get(0).statusCode(),race.get(1).statusCode())));
            assertEquals(beforeWrite+1,consumed.size());
            assertEquals(409,send(http,peer,"memory.append",append,raceGrant).statusCode());
            boolean firstWon=race.get(0).statusCode()==200;
            URI survivor=firstWon?peer:endpoint;
            Process writer=firstWon?app:replica.process();
            writer.destroyForcibly().waitFor();
            FencingFixture.recoveredOutcome(db,tenant,raceGrant,claimControl);
            FencingFixture.oneMutationAndGuardedRecords(db,tenant,"memory_message");
            byte[] receipt = body(Map.of("type", "receipt", "invocationId", fixtureClaims(raceGrant).path("invocationId").asText()));
            var committed =
                    send(
                            http,
                            survivor,
                            "memory.read",
                            receipt,
                            permit(
                                    "alice",
                                    "memory.read",
                                    receipt,
                                    tenant,
                                    System.currentTimeMillis()));
            assertEquals(200, committed.statusCode());
            assertEquals(
                    "COMMITTED", Json.MAPPER.readTree(committed.body()).path("state").asText());

            byte[] read = body(Map.of("type", "check"));
            assertEquals(
                    403,
                    send(http, survivor, "memory.read", read, null).statusCode(),
                    "A TLS peer alone grants nothing");
            assertEquals(
                    403,
                    send(
                                    http,
                                    survivor,
                                    "memory.read",
                                    read,
                                    permit(
                                            "bob",
                                            "memory.read",
                                            read,
                                            tenant,
                                            System.currentTimeMillis()))
                            .statusCode(),
                    "Valid permit plus another user's resource still denied");
            assertEquals(
                    403,
                    send(
                                    http,
                                    survivor,
                                    "memory.read",
                                    read,
                                    permit(
                                            "alice",
                                            "memory.read",
                                            read,
                                            UUID.randomUUID().toString(),
                                            System.currentTimeMillis()))
                            .statusCode());
            String valid = permit("alice", "memory.read", read, tenant, System.currentTimeMillis());
            assertEquals(200, send(http, survivor, "memory.read", read, valid).statusCode());
            assertEquals(
                    403,
                    send(http, survivor, "memory.read", body(Map.of("budget", 1)), valid)
                            .statusCode(),
                    "Exact-body binding prevents argument substitution");
            assertEquals(
                    403,
                    send(
                                    http,
                                    survivor,
                                    "memory.read",
                                    read,
                                    permit(
                                            "alice",
                                            "memory.read",
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
                    () -> send(anonymous, survivor, "memory.read", read, valid),
                    "The listener requires a real client certificate");
            epoch = 2;
            long began = System.nanoTime();
            boolean denied = false;
            while (System.nanoTime() - began < Duration.ofSeconds(8).toNanos()) {
                if (send(http, survivor, "memory.read", read, valid).statusCode() == 403) {
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
            if(replica!=null)replica.close();
            app.destroy();
            if (!app.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
                app.destroyForcibly().waitFor();
            control.stop(0);
            Arrays.fill(password, '\0');
        }
    }

    private com.fasterxml.jackson.databind.JsonNode fixtureClaims(String permit)throws Exception {
        return Json.MAPPER.readTree(Base64.getUrlDecoder().decode(permit.split("\\.")[1]));
    }

    private byte[] body(Object request) throws Exception {
        return Json.MAPPER.writeValueAsBytes(
                Map.of("conversationId", conversation, "request", Json.encode(request)));
    }

    private HttpResponse<String> send(
            HttpClient client, URI base, String operation, byte[] body, String permit)
            throws Exception {
        var request =
                HttpRequest.newBuilder(base.resolve(operation))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .header("Accept", "*/*")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (permit != null) request.header("Toolgate-Service-Permit", permit);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String permit(String subject, String operation, byte[] body, String tid, long issued)
            throws Exception {
        return permit(subject,operation,body,tid,issued,true);
    }
    private String permit(String subject,String operation,byte[] body,String tid,long issued,boolean bound)
            throws Exception {
        String authNonce=UUID.randomUUID().toString();
        boolean effect=bound&&!operation.equals("memory.read")&&!operation.equals("rag.query");
        var claims =
                new ServicePermitClaims(
                        "fixture-control",
                        ServiceAudience.olo_memory,
                        tid,
                        subject,
                        null,
                        RequestMode.HUMAN,
                        operation,
                        List.of(
                                new ResourceDescriptor(
                                        ResourceKind.CUSTOM,
                                        "olo://" + tid + "/conversation/" + conversation)),
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)),
                        executorKey,
                        "inv-"+authNonce,
                        authNonce,
                        1L,
                        issued,
                        issued + 15000,
                        "execution-fixture",
                        bound?fingerprint(operation,body):null,
                        effect?"effect-"+authNonce:null,effect?1L:null);
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

    private String fingerprint(String operation,byte[] body){
        var profile=new io.ololabs.toolgate.service.HttpRequestFingerprint.Profile(
            "sp-fixture-memory","sha256:"+"a".repeat(64),operation.substring(operation.indexOf('.')+1),
            "toolgate-service-permit:olo-memory",List.of("toolgate-service-permit"));
        return io.ololabs.toolgate.service.HttpRequestFingerprint.digest(profile,
            URI.create("https://localhost:"+servicePort+"/v1/"+operation),"POST",
            Map.of("content-type",List.of("application/json"),"accept",List.of("*/*")),body);
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
