/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.callback;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Signs worker → backend run-event callbacks, version 1. It must match
 * {@code org.olo.app.security.callback.CallbackSignature} in the olo backend.
 *
 * <pre>
 * X-OLO-Callback-Key:       key id
 * X-OLO-Callback-Timestamp: unix seconds
 * X-OLO-Callback-Signature: v1=hex(HMAC-SHA256(key, "v1\n" + timestamp + "\n" + METHOD + "\n" + path + "\n" + hex(sha256(body))))
 * </pre>
 *
 * The key comes from the worker's environment ({@code OLO_CALLBACK_KEY_ID}, {@code OLO_CALLBACK_KEY}, at least 32
 * bytes), never from workflow input, so it never lands in Temporal history.
 */
public final class CallbackSigner {

    public static final String HEADER_KEY = "X-OLO-Callback-Key";
    public static final String HEADER_TIMESTAMP = "X-OLO-Callback-Timestamp";
    public static final String HEADER_SIGNATURE = "X-OLO-Callback-Signature";
    public static final String ENV_KEY_ID = "OLO_CALLBACK_KEY_ID";
    public static final String ENV_KEY = "OLO_CALLBACK_KEY";
    public static final int MIN_KEY_BYTES = 32;

    private final String keyId;
    private final byte[] key;
    private final Clock clock;

    public CallbackSigner(String keyId, byte[] key, Clock clock) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("callback key id is required");
        }
        if (key == null || key.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException("callback key must be at least " + MIN_KEY_BYTES + " bytes");
        }
        this.keyId = keyId.trim();
        this.key = key.clone();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The signer configured in this process's environment, or empty if no key is configured. */
    public static Optional<CallbackSigner> fromEnvironment() {
        String id = setting(ENV_KEY_ID);
        String secret = setting(ENV_KEY);
        if (id == null || secret == null) {
            return Optional.empty();
        }
        return Optional.of(new CallbackSigner(id, secret.getBytes(StandardCharsets.UTF_8), Clock.systemUTC()));
    }

    /** Adds the three signature headers for a POST of {@code body} to {@code uri}. */
    public HttpRequest.Builder sign(HttpRequest.Builder request, URI uri, byte[] body) {
        long timestamp = clock.instant().getEpochSecond();
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        return request
                .header(HEADER_KEY, keyId)
                .header(HEADER_TIMESTAMP, Long.toString(timestamp))
                .header(HEADER_SIGNATURE, signature(key, timestamp, "POST", path, body));
    }

    static String signature(byte[] key, long timestamp, String method, String path, byte[] body) {
        try {
            String canonical = "v1\n" + timestamp + "\n" + method.toUpperCase() + "\n" + path + "\n"
                    + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return "v1=" + HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** System property (tests) wins over the environment variable. */
    private static String setting(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }
}
