/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.callback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackSignerTest {

    private static final String SECRET = "test-callback-secret-0123456789-abcdef";

    @AfterEach
    void clear() {
        System.clearProperty(CallbackSigner.ENV_KEY_ID);
        System.clearProperty(CallbackSigner.ENV_KEY);
    }

    /**
     * Known-answer vector, computed independently with openssl. The backend's CallbackSignatureTest asserts the same
     * value, so worker and backend provably agree.
     */
    @Test
    void matchesTheSharedReferenceVector() {
        String signature = CallbackSigner.signature(SECRET.getBytes(StandardCharsets.UTF_8), 1_700_000_000L, "POST",
                "/api/runs/run-1/events", "{\"sequenceNumber\":1}".getBytes(StandardCharsets.UTF_8));

        assertThat(signature).isEqualTo("v1=645247651af31e47409bdff025823954bea6b6940df1295f955d324e43416fe8");
    }

    @Test
    void addsKeyTimestampAndSignatureHeadersForThePathOnly() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        CallbackSigner signer = new CallbackSigner("k1", SECRET.getBytes(StandardCharsets.UTF_8), clock);
        URI uri = URI.create("http://olo:7080/api/runs/run-1/events?ignored=1");
        byte[] body = "{\"sequenceNumber\":1}".getBytes(StandardCharsets.UTF_8);

        HttpRequest request = signer.sign(HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofByteArray(body)),
                uri, body).build();

        assertThat(request.headers().firstValue(CallbackSigner.HEADER_KEY)).contains("k1");
        assertThat(request.headers().firstValue(CallbackSigner.HEADER_TIMESTAMP)).contains("1700000000");
        assertThat(request.headers().firstValue(CallbackSigner.HEADER_SIGNATURE))
                .contains("v1=645247651af31e47409bdff025823954bea6b6940df1295f955d324e43416fe8");
    }

    @Test
    void rejectsShortKeys() {
        assertThatThrownBy(() -> new CallbackSigner("k1", "short".getBytes(StandardCharsets.UTF_8), Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromEnvironmentIsEmptyWithoutAKey() {
        assertThat(CallbackSigner.fromEnvironment()).isEmpty();

        System.setProperty(CallbackSigner.ENV_KEY_ID, "k1");
        System.setProperty(CallbackSigner.ENV_KEY, SECRET);
        assertThat(CallbackSigner.fromEnvironment()).isPresent();
    }
}
