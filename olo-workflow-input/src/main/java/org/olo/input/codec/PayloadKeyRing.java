/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Payload keys by key id, and the key each tenant seals with (D7, M5.2). Loaded from a mounted secret file:
 *
 * <pre>
 * {"keys":    {"t1-2026-10": "&lt;base64 of 32 random bytes&gt;", "platform-1": "..."},
 *  "tenants": {"t1": "t1-2026-10"},
 *  "platform": "platform-1"}
 * </pre>
 *
 * A tenant without an entry seals with the platform key. Keys never leave the process and are never logged.
 */
public final class PayloadKeyRing {

    /** Environment variable naming the key file. */
    public static final String ENV_FILE = "OLO_PAYLOAD_KEYS_FILE";

    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    private final Map<String, byte[]> keys;
    private final Map<String, String> tenants;
    private final String platform;

    public PayloadKeyRing(Map<String, byte[]> keys, Map<String, String> tenants, String platform) {
        Map<String, byte[]> copy = new LinkedHashMap<>();
        keys.forEach((id, key) -> {
            if (!KEY_ID.matcher(id).matches() || key == null || key.length != 32) {
                throw new IllegalArgumentException("payload key " + id + " must be a valid id with 32 bytes");
            }
            copy.put(id, key.clone());
        });
        this.keys = Map.copyOf(copy);
        this.tenants = Map.copyOf(tenants);
        this.platform = Objects.requireNonNull(platform, "platform");
        if (!this.keys.containsKey(platform) || !this.keys.keySet().containsAll(this.tenants.values())) {
            throw new IllegalArgumentException("every active payload key must be in the ring");
        }
    }

    /** The ring in the file named by {@value #ENV_FILE}, if that variable is set. */
    public static Optional<PayloadKeyRing> fromEnvironment() {
        String file = System.getenv(ENV_FILE);
        if (file == null || file.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(Files.readString(Path.of(file.trim()))));
        } catch (IOException e) {
            throw new IllegalStateException(ENV_FILE + " cannot be read", e);
        }
    }

    public static PayloadKeyRing parse(String json) throws IOException {
        JsonNode root = new ObjectMapper().readTree(json);
        Map<String, byte[]> keys = new LinkedHashMap<>();
        root.path("keys").fields().forEachRemaining(e -> keys.put(e.getKey(), Base64.getDecoder().decode(e.getValue().asText())));
        Map<String, String> tenants = new LinkedHashMap<>();
        root.path("tenants").fields().forEachRemaining(e -> tenants.put(e.getKey(), e.getValue().asText()));
        return new PayloadKeyRing(keys, tenants, root.path("platform").asText(null));
    }

    String activeKeyId(String tenant) {
        return tenant == null ? platform : tenants.getOrDefault(tenant, platform);
    }

    byte[] key(String keyId) {
        byte[] key = keys.get(keyId);
        if (key == null) {
            throw new io.temporal.payload.codec.PayloadCodecException(
                    new java.security.GeneralSecurityException("unknown payload key " + keyId));
        }
        return key;
    }

    @Override
    public String toString() {
        return "PayloadKeyRing" + keys.keySet();
    }
}
